"""
FarCrop backend Gatekeeper — YOLO-based crop leaf validation.

Public surface:
    from gatekeeper import leaf_detector        # singleton
    from gatekeeper.result import GatekeeperResult
"""
from .leaf_detector import leaf_detector
from .result import GatekeeperResult

__all__ = ["leaf_detector", "GatekeeperResult"]
