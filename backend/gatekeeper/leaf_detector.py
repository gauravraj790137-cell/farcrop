"""
LeafDetector — singleton YOLO-based crop leaf validator.

Responsibilities
----------------
- Load backend/gatekeeper/best.pt exactly once at application startup.
- Accept a saved image path.
- Run YOLOv8 inference.
- Map detected class names to the supported-crop allowlist.
- Return a GatekeeperResult (pass / reject).

This module never predicts diseases.  It only answers the question:
"Does this image contain a supported crop leaf?"
"""
from __future__ import annotations

import logging
import time
from pathlib import Path
from typing import Any

from .result import GatekeeperResult

logger = logging.getLogger("farcrop.gatekeeper")

# ── Supported crop allowlist ──────────────────────────────────────────────────
# Keys are lower-case substrings matched against YOLO class labels from model.names.
# Values are the canonical crop names returned in GatekeeperResult.crop.
#
# IMPORTANT: The model was trained with a TYPO in bell pepper — it uses "capcicum"
# (missing the 's').  Both spellings are listed here so if a corrected model is
# dropped in later, it will still work.
_SUPPORTED_CROPS: dict[str, str] = {
    "tomato":   "tomato",
    "potato":   "potato",
    "capcicum": "capsicum",   # ← model class 24 spells it this way (typo in training data)
    "capsicum": "capsicum",   # ← correct spelling — future-proof
}


class LeafDetector:
    """
    Singleton wrapper around the YOLOv8 leaf-detection model.

    Usage
    -----
    At startup::

        leaf_detector.load()

    Per request::

        result = leaf_detector.detect(image_path)
        if not result.passed:
            # return rejection response

    Thread-safety
    -------------
    YOLO inference is CPU-bound and the GIL ensures that
    ``self._model`` is read-only after ``load()`` completes.
    Multiple async route handlers can call ``detect()`` concurrently safely.
    """

    _instance: "LeafDetector | None" = None

    def __new__(cls) -> "LeafDetector":
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self) -> None:
        if getattr(self, "_initialized", False):
            return
        self._initialized = True
        self._model: Any | None = None
        self._loaded: bool = False
        self._model_path: Path = (
            Path(__file__).resolve().parent / "best.pt"
        )

    # ── Public API ────────────────────────────────────────────────────────────

    def load(self, model_path: Path | None = None) -> None:
        """
        Load the YOLO model from disk.  Idempotent — safe to call multiple times.

        Parameters
        ----------
        model_path
            Override the default path. Useful for testing.
        """
        if self._loaded:
            return

        target = model_path or self._model_path

        if not target.exists():
            raise FileNotFoundError(
                f"Gatekeeper model not found: {target}\n"
                "Place best.pt in the gatekeeper/ directory before starting the server."
            )

        try:
            from ultralytics import YOLO  # imported here so the rest of the app
        except ImportError as exc:        # works even if ultralytics is missing
            raise RuntimeError(
                "ultralytics is not installed. Run: pip install ultralytics"
            ) from exc

        logger.info("═══════════════════════════════════════════════")
        logger.info("[GATEKEEPER] Loading model from: %s", target)
        t0 = time.perf_counter()
        self._model = YOLO(str(target))
        elapsed = time.perf_counter() - t0
        logger.info("[GATEKEEPER] Model loaded in %.2f s", elapsed)
        logger.info("[GATEKEEPER] Class names: %s", self._model.names)
        logger.info("[GATEKEEPER] Number of classes: %d", len(self._model.names))
        logger.info("═══════════════════════════════════════════════")
        self._loaded = True

    def detect(self, image_path: str) -> GatekeeperResult:
        """
        Run inference on a saved image and decide whether it is a supported crop leaf.

        Parameters
        ----------
        image_path
            Absolute or relative path to the image file on disk.

        Returns
        -------
        GatekeeperResult
            ``passed=True``  with crop name + confidence if a supported leaf is found.
            ``passed=False`` with reason + code if no supported leaf is detected.
        """
        logger.info("───────────────────────────────────────────────")
        logger.info("[GATEKEEPER] detect() called → %s", image_path)

        # ── Fail-CLOSED: if model didn't load, reject every request ─────────
        if not self._loaded or self._model is None:
            logger.error(
                "[GATEKEEPER] CRITICAL: detect() called before load() completed. "
                "Rejecting image (fail-closed)."
            )
            return GatekeeperResult.reject(
                reason="Gatekeeper model not loaded. Contact server administrator.",
                code="MODEL_NOT_LOADED",
            )

        t0 = time.perf_counter()

        try:
            results = self._model(image_path, verbose=False)
        except Exception as exc:
            logger.exception("[GATEKEEPER] YOLO inference error: %s", exc)
            # Still fail-closed: if inference crashes, reject.
            return GatekeeperResult.reject(
                reason="Inference engine error. Please retry.",
                code="INFERENCE_ERROR",
            )

        elapsed = time.perf_counter() - t0
        logger.info("[GATEKEEPER] Inference completed in %.3f s", elapsed)

        # ── Parse detections ─────────────────────────────────────────────────
        best_crop: str | None = None
        best_conf: float = 0.0
        all_detected: list[str] = []   # "label:conf" strings for logging
        all_label_names: list[str] = []  # raw label names for rejection response

        for result in results:
            boxes = result.boxes
            if boxes is None or len(boxes) == 0:
                logger.info("[GATEKEEPER] No boxes in this result frame.")
                continue

            names: dict[int, str] = result.names or {}
            logger.info("[GATEKEEPER] model.names mapping: %s", names)
            logger.info("[GATEKEEPER] Total detections in frame: %d", len(boxes))

            for i in range(len(boxes)):
                class_id = int(boxes.cls[i].item())
                conf     = float(boxes.conf[i].item())
                label    = names.get(class_id, f"class_{class_id}").lower()
                box      = boxes.xyxy[i].tolist()

                logger.info(
                    "[GATEKEEPER]   Detection #%d: class_id=%d  label='%s'  conf=%.4f  box=%s",
                    i, class_id, label, conf, [round(v, 1) for v in box]
                )

                all_detected.append(f"{label}:{conf:.3f}")
                all_label_names.append(label)

                # Check against supported crop allowlist
                matched_crop = _match_supported_crop(label)
                if matched_crop:
                    logger.info(
                        "[GATEKEEPER]   → MATCHED supported crop: '%s' (canonical='%s')",
                        label, matched_crop
                    )
                    if conf > best_conf:
                        best_crop = matched_crop
                        best_conf = conf
                else:
                    logger.info(
                        "[GATEKEEPER]   → NOT in supported crop allowlist: '%s'", label
                    )

        # ── Decision ─────────────────────────────────────────────────────────
        if not all_label_names:
            logger.info(
                "[GATEKEEPER] DECISION: REJECT — NO_LEAF_DETECTED "
                "(zero detections across all result frames)"
            )
            logger.info("───────────────────────────────────────────────")
            return GatekeeperResult.reject(
                reason="No crop leaf detected in this image.",
                code="NO_LEAF_DETECTED",
            )

        if best_crop is None:
            logger.info(
                "[GATEKEEPER] DECISION: REJECT — UNSUPPORTED_CROP "
                "detected_classes=%s  none matched allowlist",
                all_label_names,
            )
            logger.info("───────────────────────────────────────────────")
            return GatekeeperResult.reject(
                reason="Only Tomato, Potato and Bell Pepper are supported.",
                code="UNSUPPORTED_CROP",
                detected_classes=all_label_names,
                best_confidence=best_conf,
            )

        logger.info(
            "[GATEKEEPER] DECISION: PASS — crop='%s'  conf=%.4f  all_detections=[%s]",
            best_crop, best_conf, ", ".join(all_detected),
        )
        logger.info("───────────────────────────────────────────────")
        return GatekeeperResult.accept(crop=best_crop, confidence=best_conf)


# ── Module-level helpers ──────────────────────────────────────────────────────

def _match_supported_crop(label: str) -> str | None:
    """
    Return the canonical crop name if *label* matches any supported crop,
    or None if it does not.

    Matching is substring-based and case-insensitive so that labels like
    ``"tomato_leaf"`` or ``"Bell Pepper"`` are handled correctly.
    """
    label_lower = label.lower()
    for keyword, canonical in _SUPPORTED_CROPS.items():
        if keyword in label_lower:
            return canonical
    return None


# Module-level singleton — import this everywhere instead of instantiating directly.
leaf_detector = LeafDetector()
