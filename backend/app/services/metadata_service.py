from __future__ import annotations

from typing import Any


class DiseaseMetadataService:
    """Provide disease metadata without coupling the route to the disease labels."""

    def __init__(self) -> None:
        self._database: dict[str, dict[str, Any]] = {
            "Tomato___Early_blight": {
                "cause": "Fungal",
                "description": "A common fungal disease that causes dark lesions with concentric rings on tomato leaves.",
                "treatment": [
                    "Remove infected leaves",
                    "Avoid overhead watering",
                    "Spray Mancozeb",
                ],
                "recommended_products": ["Mancozeb", "Copper Fungicide"],
            },
            "Tomato___Late_blight": {
                "cause": "Fungal",
                "description": "A destructive disease that causes large irregular lesions and can spread quickly in humid weather.",
                "treatment": [
                    "Remove infected foliage",
                    "Improve airflow",
                    "Apply a suitable fungicide",
                ],
                "recommended_products": ["Copper Fungicide", "Mancozeb"],
            },
            "Tomato___healthy": {
                "cause": "None",
                "description": "The crop appears healthy and does not show any disease symptoms.",
                "treatment": ["Continue routine crop monitoring and nutrition management."],
                "recommended_products": ["Balanced fertilizer"],
            },
            "Potato___Early_blight": {
                "cause": "Fungal",
                "description": "A foliar disease with dark lesions that can reduce photosynthesis and yield.",
                "treatment": [
                    "Remove infected foliage",
                    "Use disease-free seed",
                    "Apply preventive fungicides",
                ],
                "recommended_products": ["Mancozeb", "Chlorothalonil"],
            },
            "Potato___Late_blight": {
                "cause": "Oomycete",
                "description": "A fast-spreading disease that can devastate potato plants under wet conditions.",
                "treatment": [
                    "Avoid wet foliage",
                    "Improve air circulation",
                    "Apply a recommended fungicide",
                ],
                "recommended_products": ["Copper-based fungicide", "Mancozeb"],
            },
            "Potato___healthy": {
                "cause": "None",
                "description": "The potato crop appears healthy and free from disease symptoms.",
                "treatment": ["Continue field monitoring and balanced nutrition."],
                "recommended_products": ["Potassium fertilizer"],
            },
            "Pepper__bell___Bacterial_spot": {
                "cause": "Bacterial",
                "description": "A bacterial disease that produces circular spots on leaves and fruit.",
                "treatment": [
                    "Remove infected leaves",
                    "Avoid overhead irrigation",
                    "Use clean tools and seed",
                ],
                "recommended_products": ["Copper bactericide", "Disinfectant"],
            },
            "Pepper__bell___healthy": {
                "cause": "None",
                "description": "The pepper plant appears healthy and free from disease symptoms.",
                "treatment": ["Maintain regular monitoring and crop nutrition."],
                "recommended_products": ["Balanced fertilizer"],
            },
            "Unknown": {
                "cause": "Unknown",
                "description": "No disease information is available for this prediction.",
                "treatment": ["Inspect the crop closely and consult a local agronomist."],
                "recommended_products": ["General crop disinfectant"],
            },
        }

    def enrich(self, prediction: dict[str, Any]) -> dict[str, Any]:
        disease_name = str(prediction.get("disease", "Unknown"))
        disease_info = self._database.get(disease_name, self._database["Unknown"])
        return {
            "disease": disease_name,
            "confidence": float(prediction.get("confidence", 0.0)),
            "cause": disease_info["cause"],
            "description": disease_info["description"],
            "treatment": disease_info["treatment"],
            "products": disease_info["recommended_products"],
        }
