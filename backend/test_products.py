"""
Backend product search smoke test.
Run from project root:
    .venv\Scripts\python.exe backend\test_products.py
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))

from app.services.llm_service import LLMService
from app.config import SERPAPI_API_KEY, SERPAPI_COUNTRY, SERPAPI_LOCATION

print("=" * 60)
print(" PRODUCT SEARCH SMOKE TEST")
print("=" * 60)
print(f"  SERPAPI_API_KEY  : {'SET (' + SERPAPI_API_KEY[:6] + '...)' if SERPAPI_API_KEY else 'NOT SET'}")
print(f"  SERPAPI_COUNTRY  : {SERPAPI_COUNTRY}")
print(f"  SERPAPI_LOCATION : {SERPAPI_LOCATION}")
print()

svc = LLMService()

# ── Test 1: get_serpapi_products with no key ──────────────────────────────────
print("TEST 1 — get_serpapi_products with no/missing key")
result = svc.get_serpapi_products(["Mancozeb"])
print(f"  Result: {result}   (expected [] if no API key)")
print()

# ── Test 2: generate() with no Groq + no Serp ────────────────────────────────
print("TEST 2 — generate() with no keys (graceful degradation)")
mock_prediction = {
    "disease":     "Tomato___Early_blight",
    "confidence":  0.93,
    "cause":       "Fungal",
    "description": "A common fungal disease.",
    "treatment":   ["Remove infected leaves", "Spray Mancozeb"],
    "products":    ["Mancozeb", "Copper Fungicide"],
    "explanation": "",
}
result = svc.generate(mock_prediction, crop_cycle=None)
print(f"  Keys returned  : {list(result.keys())}")
print(f"  explanation    : {result.get('explanation', '')[:80]}")
print(f"  buy_links type : {type(result.get('buy_links'))}")
print(f"  buy_links count: {len(result.get('buy_links', []))}")
if result.get("buy_links"):
    first = result["buy_links"][0]
    print(f"  First product  : {first}")
    expected_keys = {"title", "url"}
    assert expected_keys.issubset(first.keys()), f"Missing keys: {expected_keys - first.keys()}"
    print("  [PASS] title and url present")
print()

# ── Test 3: Live SerpAPI search (only if key is set) ─────────────────────────
if SERPAPI_API_KEY:
    print("TEST 3 — Live SerpAPI search")
    products = svc.get_serpapi_products(["Mancozeb fungicide", "Copper Fungicide"])
    print(f"  Products found: {len(products)}")
    for p in products:
        print(f"    - {p.get('title', '?')[:50]:50s}  brand={p.get('brand','?')[:20]:20s}  price={p.get('price','?')}")
    print()
    all_have_url = all(p.get("url") for p in products)
    print(f"  [{'PASS' if all_have_url else 'FAIL'}] All products have url")
else:
    print("TEST 3 — Skipped (SERPAPI_API_KEY not set)")

print()
print("=" * 60)
print(" SMOKE TEST COMPLETE")
print("=" * 60)
