import logging
from pathlib import Path
from typing import Any

logger = logging.getLogger("farcrop.inference")


class CropDiseaseModel:
    """Load a local Hugging Face image-classification model once and reuse it."""

    _instance: "CropDiseaseModel | None" = None
    _LABEL_NORMALIZATION_MAP = {
        "Tomato with Early Blight": "Tomato___Early_blight",
        "Tomato with Late Blight": "Tomato___Late_blight",
        "Healthy Tomato Plant": "Tomato___healthy",
        "Potato with Early Blight": "Potato___Early_blight",
        "Potato with Late Blight": "Potato___Late_blight",
        "Healthy Potato Plant": "Potato___healthy",
        "Bell Pepper with Bacterial Spot": "Pepper__bell___Bacterial_spot",
        "Healthy Bell Pepper Plant": "Pepper__bell___healthy",
    }

    def __new__(cls) -> "CropDiseaseModel":
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self) -> None:
        if getattr(self, "_initialized", False):
            return

        self._initialized = True
        self.model: Any | None = None
        self.processor: Any | None = None
        self._loaded = False
        self.device: str = "cpu"
        self.model_dir = self._resolve_model_dir()

    def _resolve_model_dir(self) -> Path:
        """Locate the local model directory from common backend or workspace paths."""
        candidates = [
            Path(__file__).resolve().parents[2] / "models" / "crop_model",
            Path(__file__).resolve().parents[2] / "crop",
            Path(__file__).resolve().parents[3] / "crop",
        ]
        for candidate in candidates:
            if candidate.exists() and (candidate / "config.json").exists():
                return candidate
        return candidates[0]

    def _resolve_weight_path(self, model_dir: Path | None = None) -> Path | None:
        """Resolve a supported local weight file for the model directory."""
        base_dir = model_dir or self.model_dir
        for candidate_name in ("model.safetensors", "pytorch_model.bin"):
            candidate_path = base_dir / candidate_name
            if candidate_path.exists():
                return candidate_path
        return None

    def _normalize_prediction_label(self, label: str) -> str:
        """Map the new model's label names to the disease metadata keys used by the service."""
        normalized_label = str(label).strip()
        if not normalized_label:
            return "Unknown"
        return self._LABEL_NORMALIZATION_MAP.get(normalized_label, normalized_label)

    def load_model(self) -> None:
        """Load the processor and model from the local model directory."""
        if self._loaded:
            return

        if not self.model_dir.exists():
            raise FileNotFoundError(f"Model directory not found: {self.model_dir}")

        required_files = ["config.json", "preprocessor_config.json"]
        missing_files = [name for name in required_files if not (self.model_dir / name).exists()]
        if missing_files:
            missing = ", ".join(missing_files)
            raise FileNotFoundError(f"Missing model files: {missing}")

        weight_path = self._resolve_weight_path(self.model_dir)
        if weight_path is None:
            raise FileNotFoundError("Missing model weights: expected model.safetensors or pytorch_model.bin")

        try:
            from transformers import AutoImageProcessor, AutoModelForImageClassification
            import torch
        except ImportError as exc:  # pragma: no cover - depends on environment
            raise RuntimeError("Transformers or PyTorch is not installed.") from exc

        try:
            self.processor = AutoImageProcessor.from_pretrained(str(self.model_dir), local_files_only=True)
            self.model = AutoModelForImageClassification.from_pretrained(str(self.model_dir), local_files_only=True)
        except Exception as exc:  # pragma: no cover - runtime dependency path
            raise RuntimeError(f"Unable to load model from {self.model_dir}") from exc

        self.device = torch.device("cpu")
        self.model.to(self.device)
        self.model.eval()
        self._loaded = True
        logger.info("Model Loaded Successfully")

    def predict(self, image_path: str) -> dict[str, Any]:
        """Run inference on an image and return disease plus confidence."""
        if not self._loaded:
            self.load_model()

        if self.model is None or self.processor is None:
            raise RuntimeError("Model is not ready for inference.")

        try:
            from PIL import Image, UnidentifiedImageError
            import torch
        except ImportError as exc:  # pragma: no cover - depends on environment
            raise RuntimeError("Pillow or PyTorch is not installed.") from exc

        try:
            image = Image.open(image_path).convert("RGB")
        except FileNotFoundError as exc:
            raise FileNotFoundError(f"Image file not found: {image_path}") from exc
        except UnidentifiedImageError as exc:
            raise ValueError("Corrupt or invalid image file.") from exc
        except OSError as exc:
            raise ValueError("Corrupt or invalid image file.") from exc

        try:
            inputs = self.processor(images=image, return_tensors="pt")
            if hasattr(inputs, "to"):
                inputs = inputs.to(self.device)
            else:
                inputs = {key: value.to(self.device) for key, value in inputs.items()}

            with torch.no_grad():
                outputs = self.model(**inputs)

            probabilities = torch.nn.functional.softmax(outputs.logits, dim=-1)[0]
            confidence_value, predicted_index = probabilities.max(dim=0)
            predicted_label = self.model.config.id2label.get(int(predicted_index.item()), str(predicted_index.item()))
            predicted_label = self._normalize_prediction_label(predicted_label)
            confidence = round(float(confidence_value.item()) * 100, 2)
        except Exception as exc:  # pragma: no cover - runtime dependency path
            raise RuntimeError("Inference failed.") from exc

        return {
            "disease": predicted_label,
            "confidence": confidence,
        }


crop_disease_model = CropDiseaseModel()


def predict(image_path: str) -> dict[str, Any]:
    """Expose the singleton model prediction interface for the route layer."""
    return crop_disease_model.predict(image_path)
