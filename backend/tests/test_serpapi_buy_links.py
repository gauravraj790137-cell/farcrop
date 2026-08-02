import sys
from types import SimpleNamespace

import app.services.llm_service as llm_service_module


class FakeSerpapiClient:
    def __init__(self, api_key: str):
        self.api_key = api_key

    def search(self, payload):
        assert payload["engine"] == "google_shopping"
        return {
            "shopping_results": [
                {
                    "title": "Best Buy product",
                    "link": "https://example.com/best-buy",
                    "thumbnail": "https://example.com/thumb.png",
                }
            ]
        }


def test_get_serpapi_buy_links_uses_shopping_results(monkeypatch):
    fake_module = SimpleNamespace(Client=FakeSerpapiClient)
    monkeypatch.setitem(sys.modules, "serpapi", fake_module)
    monkeypatch.setattr(llm_service_module, "SERPAPI_API_KEY", "test-key")
    monkeypatch.setattr(llm_service_module, "SERPAPI_LOCATION", "Austin")
    monkeypatch.setattr(llm_service_module, "SERPAPI_LANGUAGE", "en")
    monkeypatch.setattr(llm_service_module, "SERPAPI_COUNTRY", "us")

    service = llm_service_module.LLMService()
    links = service.get_serpapi_buy_links(["Mancozeb"])

    assert len(links) == 1
    assert links[0]["title"] == "Best Buy product"
    assert links[0]["url"] == "https://example.com/best-buy"
    assert links[0]["thumbnail"] == "https://example.com/thumb.png"
