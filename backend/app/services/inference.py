from typing import Any


def predict(image_path: str) -> dict[str, Any]:
    """Return placeholder inference output for a crop image path.

    This function is intentionally isolated so the AI model can be swapped here later
    without changing any API routes.
    """
    return {
        "disease": "Tomato Early Blight",
        "confidence": 96.42,
    }
