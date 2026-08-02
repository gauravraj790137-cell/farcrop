from __future__ import annotations

from typing import Any

from gatekeeper import leaf_detector
from gatekeeper.result import GatekeeperResult


class GatekeeperService:
    """Wrap the YOLO gatekeeper so the route layer stays orchestrator-focused."""

    def validate_crop(self, image_path: str) -> GatekeeperResult:
        return leaf_detector.detect(image_path)
