"""
GatekeeperResult — plain data object returned by LeafDetector.detect().

Kept deliberately simple so the route layer can convert it to whatever
API response shape it needs without the Gatekeeper caring about HTTP.
"""
from dataclasses import dataclass, field


@dataclass(frozen=True)
class GatekeeperResult:
    """
    Result of a single Gatekeeper inference pass.

    Attributes
    ----------
    passed : bool
        True  → a supported crop leaf was detected; proceed with disease inference.
        False → no supported crop found; abort and return a rejection response.
    crop : str | None
        Detected crop class name (e.g. "tomato") when passed is True, else None.
    confidence : float
        Highest detection confidence (0.0–1.0) for the winning class.
        0.0 when passed is False.
    reason : str | None
        Human-readable rejection reason when passed is False, else None.
    code : str
        Machine-readable rejection code. One of:
          - "NO_LEAF_DETECTED"  — YOLO found zero detections.
          - "UNSUPPORTED_CROP"  — Detections exist but none matched the allowlist.
          - "INFERENCE_ERROR"   — YOLO crashed during inference.
          - "MODEL_NOT_LOADED"  — Model was never initialised.
        Empty string when passed is True.
    detected_classes : list[str]
        All class labels detected (for UNSUPPORTED_CROP responses).
        Empty when passed is True or code is NO_LEAF_DETECTED.
    """

    passed: bool
    crop: str | None
    confidence: float
    reason: str | None
    code: str = ""
    detected_classes: tuple[str, ...] = field(default_factory=tuple)

    # ── Convenience constructors ──────────────────────────────────────────────

    @classmethod
    def accept(cls, crop: str, confidence: float) -> "GatekeeperResult":
        """Return a passing result for a detected crop."""
        return cls(passed=True, crop=crop, confidence=confidence, reason=None, code="")

    @classmethod
    def reject(
        cls,
        reason: str,
        code: str = "REJECTED",
        detected_classes: list[str] | None = None,
        best_confidence: float = 0.0,
    ) -> "GatekeeperResult":
        """Return a failing result with an explanation and machine-readable code."""
        return cls(
            passed=False,
            crop=None,
            confidence=best_confidence,
            reason=reason,
            code=code,
            detected_classes=tuple(detected_classes or []),
        )
