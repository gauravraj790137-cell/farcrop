from __future__ import annotations

from typing import Any

from .inference import predict


class DiseaseInferenceService:
    """Isolation layer for the disease classifier so the route orchestrates it cleanly."""

    def infer(self, image_path: str) -> dict[str, Any]:
        return predict(image_path)
