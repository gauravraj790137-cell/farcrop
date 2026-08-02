from __future__ import annotations

import json
import logging
from datetime import datetime, timezone
from typing import Any

from ..config import (
    GROQ_API_KEY,
    GROQ_MODEL,
    LLM_TIMEOUT_SECONDS,
    SERPAPI_API_KEY,
    SERPAPI_COUNTRY,
    SERPAPI_LANGUAGE,
    SERPAPI_LOCATION,
)

logger = logging.getLogger("farcrop.llm")


# ── Product schema returned by get_serpapi_products ───────────────────────────
# Each element is a dict with these keys (all strings, all optional except title/url):
#   title      – product name
#   brand      – seller / brand name
#   thumbnail  – image URL
#   price      – formatted price string  (e.g. "₹450" or "$12.99")
#   rating     – star rating string       (e.g. "4.5")
#   url        – product / affiliate link
# --------------------------------------------------------------------------

_PRODUCT_SEARCH_LIMIT = 3   # max product names to search
_RESULTS_PER_QUERY    = 5   # max products to return per search query


class LLMService:
    """Generate farmer-friendly explanations using the disease prediction and cycle metadata."""

    # ── Public API ────────────────────────────────────────────────────────────

    def build_context(self, prediction: dict[str, Any], crop_cycle: dict[str, Any] | None = None) -> dict[str, Any]:
        return {
            "prediction": prediction,
            "crop_cycle": crop_cycle or {},
            "generated_at": datetime.now(timezone.utc).isoformat(),
        }

    def get_serpapi_products(self, product_names: list[str]) -> list[dict[str, str]]:
        """
        Search Google Shopping via SerpAPI for each product name and return
        a structured list with title, brand, thumbnail, price, rating, url.

        Returns an empty list on any error — never raises.
        """
        if not SERPAPI_API_KEY:
            logger.warning("[SerpAPI] SERPAPI_API_KEY is not set — skipping product search.")
            return []

        try:
            import serpapi
        except ImportError:
            logger.warning("[SerpAPI] serpapi package not installed — skipping product search.")
            return []

        all_products: list[dict[str, str]] = []

        try:
            client = serpapi.Client(api_key=SERPAPI_API_KEY)

            for product_name in product_names[:_PRODUCT_SEARCH_LIMIT]:
                logger.info("[SerpAPI] Searching for: %r  (country=%s)", product_name, SERPAPI_COUNTRY)

                try:
                    results = client.search({
                        "engine":        "google_shopping",
                        "q":             f"{product_name} agricultural fungicide pesticide",
                        "location":      SERPAPI_LOCATION,
                        "hl":            SERPAPI_LANGUAGE,
                        "gl":            SERPAPI_COUNTRY,
                        "google_domain": "google.co.in",
                        "num":           "10",
                    })
                except Exception as search_exc:
                    logger.error("[SerpAPI] Search failed for %r: %s", product_name, search_exc)
                    continue

                # Try shopping_results first (richest data), then inline_shopping_results,
                # then organic_results as last resort (rarely has thumbnails).
                items = (
                    results.get("shopping_results")
                    or results.get("inline_shopping_results")
                    or results.get("organic_results")
                    or []
                )

                logger.info(
                    "[SerpAPI] %r → %d raw result(s) from %s",
                    product_name,
                    len(items),
                    "shopping_results" if results.get("shopping_results")
                    else "inline_shopping_results" if results.get("inline_shopping_results")
                    else "organic_results",
                )

                count = 0
                for item in items:
                    if count >= _RESULTS_PER_QUERY:
                        break

                    url = (
                        item.get("link")
                        or item.get("url")
                        or item.get("product_link")
                        or ""
                    )
                    title = item.get("title") or product_name
                    if not url or not title:
                        continue

                    product: dict[str, str] = {
                        "title": str(title).strip(),
                        "url":   str(url).strip(),
                    }

                    # Brand / source
                    brand = (
                        item.get("source")
                        or item.get("brand")
                        or item.get("seller")
                        or item.get("merchant", {}).get("name", "") if isinstance(item.get("merchant"), dict) else ""
                        or ""
                    )
                    if brand:
                        product["brand"] = str(brand).strip()

                    # Thumbnail
                    thumbnail = (
                        item.get("thumbnail")
                        or item.get("image")
                        or item.get("thumbnail_url")
                        or ""
                    )
                    if thumbnail:
                        product["thumbnail"] = str(thumbnail).strip()

                    # Price
                    price = (
                        item.get("price")
                        or item.get("extracted_price")
                        or ""
                    )
                    if price:
                        product["price"] = str(price).strip()

                    # Rating
                    rating = item.get("rating") or item.get("reviews_rating") or ""
                    if rating:
                        product["rating"] = str(rating).strip()

                    logger.info(
                        "[SerpAPI]   product: title=%r  brand=%r  price=%r  rating=%r  has_thumb=%s",
                        product.get("title"), product.get("brand"), product.get("price"),
                        product.get("rating"), bool(product.get("thumbnail")),
                    )

                    all_products.append(product)
                    count += 1

        except Exception as exc:
            logger.error("[SerpAPI] Unexpected error during product search: %s", exc, exc_info=True)
            return []

        logger.info("[SerpAPI] Total products collected: %d", len(all_products))
        return all_products

    # ── keep old name as alias so nothing else breaks ─────────────────────────
    def get_serpapi_buy_links(self, product_names: list[str]) -> list[dict[str, str]]:
        return self.get_serpapi_products(product_names)

    def generate(self, prediction: dict[str, Any], crop_cycle: dict[str, Any] | None = None) -> dict[str, Any]:
        product_names = [
            str(item)
            for item in (prediction.get("products", []) or [])
            if isinstance(item, str) and item
        ]

        if not GROQ_API_KEY:
            logger.warning("[LLM] GROQ_API_KEY not set — returning metadata-only explanation.")
            return {
                "explanation": "AI explanation is unavailable (no API key configured).",
                "buy_links":   self.get_serpapi_products(product_names),
            }

        try:
            from groq import Groq
        except ImportError:
            logger.warning("[LLM] groq package not installed.")
            return {
                "explanation": "LLM integration is unavailable (groq package not installed).",
                "buy_links":   self.get_serpapi_products(product_names),
            }

        client  = Groq(api_key=GROQ_API_KEY, timeout=LLM_TIMEOUT_SECONDS)
        context = self.build_context(prediction, crop_cycle)
        prompt  = (
            "You are a helpful agricultural assistant for Indian farmers. "
            "Return compact JSON with exactly two fields: "
            "\"explanation\" (2-3 farmer-friendly sentences about the disease and what to do) and "
            "\"buy_links\" (empty list — leave this empty, products are handled separately). "
            f"Context: {json.dumps(context, ensure_ascii=False)}"
        )

        try:
            completion = client.chat.completions.create(
                model=GROQ_MODEL,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.2,
                max_completion_tokens=512,
                top_p=1,
                stream=False,
            )
            raw_content = completion.choices[0].message.content or "{}"

            # Strip markdown code fences if LLM wraps response in ```json ... ```
            raw_content = raw_content.strip()
            if raw_content.startswith("```"):
                raw_content = raw_content.split("```", 2)[-1] if raw_content.count("```") >= 2 else raw_content
                if raw_content.startswith("json"):
                    raw_content = raw_content[4:]
                raw_content = raw_content.rstrip("`").strip()

            payload     = json.loads(raw_content)
            explanation = str(payload.get("explanation", "No explanation available."))

            logger.info("[LLM] Explanation generated (%d chars)", len(explanation))

        except json.JSONDecodeError:
            logger.warning("[LLM] Could not parse LLM JSON response — using raw text.")
            explanation = raw_content[:500] if raw_content else "Explanation could not be generated."
        except Exception as exc:
            logger.error("[LLM] Generation failed: %s", exc)
            explanation = "AI explanation could not be generated at this moment."

        # Always run SerpAPI regardless of LLM success
        serp_products = self.get_serpapi_products(product_names)

        return {
            "explanation": explanation,
            "buy_links":   serp_products,
        }
