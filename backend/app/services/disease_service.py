import json
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


class DiseaseService:
    """Provide disease metadata for the model prediction labels."""

    def __init__(self) -> None:
        self.database: dict[str, dict[str, Any]] = {
            "Tomato___Early_blight": {
                "cause": "Fungal",
                "description": "A common fungal disease that causes dark lesions with concentric rings on tomato leaves.",
                "treatment": [
                    "Remove infected leaves",
                    "Avoid overhead watering",
                    "Spray Mancozeb",
                ],
                "recommended_products": ["Mancozeb", "Copper Fungicide"],
            },
            "Tomato___Late_blight": {
                "cause": "Fungal",
                "description": "A destructive disease that causes large irregular lesions and can spread quickly in humid weather.",
                "treatment": [
                    "Remove infected foliage",
                    "Improve airflow",
                    "Apply a suitable fungicide",
                ],
                "recommended_products": ["Copper Fungicide", "Mancozeb"],
            },
            "Tomato___healthy": {
                "cause": "None",
                "description": "The crop appears healthy and does not show any disease symptoms.",
                "treatment": ["Continue routine crop monitoring and nutrition management."],
                "recommended_products": ["Balanced fertilizer"],
            },
            "Potato___Early_blight": {
                "cause": "Fungal",
                "description": "A foliar disease with dark lesions that can reduce photosynthesis and yield.",
                "treatment": [
                    "Remove infected foliage",
                    "Use disease-free seed",
                    "Apply preventive fungicides",
                ],
                "recommended_products": ["Mancozeb", "Chlorothalonil"],
            },
            "Potato___Late_blight": {
                "cause": "Oomycete",
                "description": "A fast-spreading disease that can devastate potato plants under wet conditions.",
                "treatment": [
                    "Avoid wet foliage",
                    "Improve air circulation",
                    "Apply a recommended fungicide",
                ],
                "recommended_products": ["Copper-based fungicide", "Mancozeb"],
            },
            "Potato___healthy": {
                "cause": "None",
                "description": "The potato crop appears healthy and free from disease symptoms.",
                "treatment": ["Continue field monitoring and balanced nutrition."],
                "recommended_products": ["Potassium fertilizer"],
            },
            "Pepper__bell___Bacterial_spot": {
                "cause": "Bacterial",
                "description": "A bacterial disease that produces circular spots on leaves and fruit.",
                "treatment": [
                    "Remove infected leaves",
                    "Avoid overhead irrigation",
                    "Use clean tools and seed",
                ],
                "recommended_products": ["Copper bactericide", "Disinfectant"],
            },
            "Pepper__bell___healthy": {
                "cause": "None",
                "description": "The pepper plant appears healthy and free from disease symptoms.",
                "treatment": ["Maintain regular monitoring and crop nutrition."],
                "recommended_products": ["Balanced fertilizer"],
            },
            "Unknown": {
                "cause": "Unknown",
                "description": "No disease information is available for this prediction.",
                "treatment": ["Inspect the crop closely and consult a local agronomist."],
                "recommended_products": ["General crop disinfectant"],
            },
        }

    def get_details(self, prediction: dict[str, Any]) -> dict[str, Any]:
        """Enrich a model prediction with disease metadata."""
        disease_name = str(prediction.get("disease", "Unknown"))
        disease_info = self.database.get(disease_name, self.database["Unknown"])

        return {
            "disease": disease_name,
            "confidence": float(prediction.get("confidence", 0.0)),
            "cause": disease_info["cause"],
            "description": disease_info["description"],
            "treatment": disease_info["treatment"],
            "products": disease_info["recommended_products"],
        }

    def build_llm_context(self, details: dict[str, Any]) -> str:
        """Create a compact prompt context for the LLM service."""
        return json.dumps(details, ensure_ascii=False)

    def get_serpapi_links(self, product_names: list[str]) -> list[dict[str, str]]:
        """Search for shopping links for suggested products using SerpAPI when configured."""
        if not SERPAPI_API_KEY:
            return []

        try:
            import serpapi
        except ImportError as exc:  # pragma: no cover - runtime dependency path
            return []

        links: list[dict[str, str]] = []
        try:
            client = serpapi.Client(api_key=SERPAPI_API_KEY)
            for product_name in product_names[:3]:
                results = client.search(
                    {
                        "engine": "google_shopping",
                        "q": product_name,
                        "location": SERPAPI_LOCATION,
                        "hl": SERPAPI_LANGUAGE,
                        "gl": SERPAPI_COUNTRY,
                        "google_domain": "google.com",
                    }
                )
                for item in (results.get("shopping_results") or results.get("organic_results") or [])[:3]:
                    link = item.get("link") or item.get("url") or ""
                    title = item.get("title") or product_name
                    if not link:
                        continue

                    payload: dict[str, str] = {"title": str(title), "url": str(link)}
                    thumbnail = item.get("thumbnail") or item.get("image") or item.get("thumbnail_url") or ""
                    if thumbnail:
                        payload["thumbnail"] = str(thumbnail)
                    links.append(payload)
        except Exception:
            return []

        return links

    def get_llm_response(self, details: dict[str, Any]) -> dict[str, Any]:
        """Generate an explanation and buy-link suggestions using Groq if configured."""
        if not GROQ_API_KEY:
            return {
                "explanation": "LLM explanation is unavailable because no Groq API key is configured.",
                "buy_links": [],
            }

        try:
            from groq import Groq
        except ImportError as exc:  # pragma: no cover - runtime dependency path
            return {
                "explanation": "LLM integration is unavailable because the groq package is not installed.",
                "buy_links": [],
            }

        client = Groq(api_key=GROQ_API_KEY, timeout=LLM_TIMEOUT_SECONDS)
        prompt = (
            "You are a helpful agricultural assistant. Return compact JSON with two fields: "
            "explanation and buy_links. explanation should be a farmer-friendly explanation in 2-3 sentences. "
            "buy_links should be a list of objects with title and url for suggested products if known, otherwise empty. "
            f"Context: {self.build_llm_context(details)}"
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
            content = completion.choices[0].message.content or "{}"
            payload = json.loads(content)
            explanation = payload.get("explanation", "No explanation available.")
            buy_links = payload.get("buy_links", [])
            llm_links = [
                {"title": str(item.get("title", "Product")), "url": str(item.get("url", ""))}
                for item in buy_links if isinstance(item, dict)
            ]
            product_names = [item.get("title", "") for item in llm_links if item.get("title")]
            serp_links = self.get_serpapi_links(product_names)
            return {
                "explanation": str(explanation),
                "buy_links": llm_links + serp_links,
            }
        except Exception:
            return {
                "explanation": "LLM explanation could not be generated at the moment.",
                "buy_links": [],
            }


disease_service = DiseaseService()


def enrich_prediction(prediction: dict[str, Any]) -> dict[str, Any]:
    """Expose the disease detail enrichment helper for the route layer."""
    details = disease_service.get_details(prediction)
    llm_payload = disease_service.get_llm_response(details)
    details.update(llm_payload)
    return details
