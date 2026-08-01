from typing import Any


DISEASE_DATABASE: dict[str, dict[str, Any]] = {
    "Tomato Early Blight": {
        "cause": "Fungal",
        "description": "A common fungal disease that causes dark lesions with concentric rings on tomato leaves.",
        "treatment": [
            "Remove infected leaves",
            "Avoid overhead watering",
            "Spray Mancozeb",
        ],
        "recommended_products": ["Mancozeb", "Copper Fungicide"],
    },
    "Unknown": {
        "cause": "Unknown",
        "description": "No disease information is available for this prediction.",
        "treatment": ["Inspect the crop closely and consult a local agronomist."],
        "recommended_products": ["General crop disinfectant"],
    },
}


def enrich_prediction(prediction: dict[str, Any]) -> dict[str, Any]:
    """Enrich a minimal inference result with disease metadata."""
    disease_name = prediction.get("disease", "Unknown")
    disease_info = DISEASE_DATABASE.get(disease_name, DISEASE_DATABASE["Unknown"])

    return {
        "disease": disease_name,
        "confidence": float(prediction.get("confidence", 0.0)),
        "cause": disease_info["cause"],
        "description": disease_info["description"],
        "treatment": disease_info["treatment"],
        "recommended_products": disease_info["recommended_products"],
    }
