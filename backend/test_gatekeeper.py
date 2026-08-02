"""
Gatekeeper end-to-end smoke test.
Run from the project root:
    .venv\Scripts\python.exe backend\test_gatekeeper.py

Pass image paths as CLI arguments:
    .venv\Scripts\python.exe backend\test_gatekeeper.py path\to\tomato.jpg path\to\wheat.jpg
"""
from __future__ import annotations

import sys
import time
import logging
from pathlib import Path

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s  %(levelname)-8s  %(name)s  %(message)s",
)

# Make sure backend/ is importable
_BACKEND = Path(__file__).resolve().parent
_PROJECT = _BACKEND.parent
sys.path.insert(0, str(_BACKEND))

from gatekeeper.leaf_detector import leaf_detector

MODEL_PATH = _PROJECT / "gatekeeper" / "best.pt"

print()
print("=" * 60)
print(" GATEKEEPER SMOKE TEST")
print("=" * 60)
print(f"  Model path : {MODEL_PATH}")
print(f"  Exists     : {MODEL_PATH.exists()}")
print()

# Load
t0 = time.perf_counter()
leaf_detector.load(model_path=MODEL_PATH)
print(f"  [OK] Model loaded in {time.perf_counter()-t0:.2f}s")
print()

# Test images from CLI, or fall back to a message
images = sys.argv[1:] if len(sys.argv) > 1 else []

if not images:
    print("  No test images provided. Pass image paths as arguments:")
    print("  .venv\\Scripts\\python.exe backend\\test_gatekeeper.py img1.jpg img2.jpg")
    print()
    sys.exit(0)

print(f"  Testing {len(images)} image(s)...")
print("-" * 60)

for img_path in images:
    p = Path(img_path)
    if not p.exists():
        print(f"  [SKIP] {img_path} — file not found")
        continue

    print(f"\n  Image: {p.name}")
    t_inf = time.perf_counter()
    result = leaf_detector.detect(str(p))
    elapsed = time.perf_counter() - t_inf

    if result.passed:
        print(f"  ✅ PASS — crop='{result.crop}'  conf={result.confidence:.4f}  ({elapsed*1000:.0f}ms)")
    else:
        print(f"  ❌ REJECT — code='{result.code}'  reason='{result.reason}'")
        if result.detected_classes:
            print(f"     detected_classes={list(result.detected_classes)}")
        print(f"     ({elapsed*1000:.0f}ms)")

print()
print("=" * 60)
print(" TEST COMPLETE")
print("=" * 60)
