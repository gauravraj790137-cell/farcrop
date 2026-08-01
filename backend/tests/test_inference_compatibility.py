import tempfile
import unittest
from pathlib import Path

from app.services.inference import CropDiseaseModel


class CropDiseaseModelCompatibilityTests(unittest.TestCase):
    def test_normalizes_model_labels_to_existing_metadata_keys(self) -> None:
        model = CropDiseaseModel()

        self.assertEqual(model._normalize_prediction_label("Tomato with Early Blight"), "Tomato___Early_blight")
        self.assertEqual(model._normalize_prediction_label("Tomato with Late Blight"), "Tomato___Late_blight")
        self.assertEqual(model._normalize_prediction_label("Healthy Tomato Plant"), "Tomato___healthy")
        self.assertEqual(model._normalize_prediction_label("Potato with Early Blight"), "Potato___Early_blight")
        self.assertEqual(model._normalize_prediction_label("Bell Pepper with Bacterial Spot"), "Pepper__bell___Bacterial_spot")
        self.assertEqual(model._normalize_prediction_label("Healthy Bell Pepper Plant"), "Pepper__bell___healthy")

    def test_resolves_supported_model_weight_files(self) -> None:
        model = CropDiseaseModel()

        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            (tmp_path / "config.json").write_text("{}", encoding="utf-8")
            (tmp_path / "preprocessor_config.json").write_text("{}", encoding="utf-8")
            (tmp_path / "model.safetensors").write_text("dummy", encoding="utf-8")
            self.assertEqual(model._resolve_weight_path(tmp_path), tmp_path / "model.safetensors")

        with tempfile.TemporaryDirectory() as tmp_dir:
            tmp_path = Path(tmp_dir)
            (tmp_path / "config.json").write_text("{}", encoding="utf-8")
            (tmp_path / "preprocessor_config.json").write_text("{}", encoding="utf-8")
            (tmp_path / "pytorch_model.bin").write_text("dummy", encoding="utf-8")
            self.assertEqual(model._resolve_weight_path(tmp_path), tmp_path / "pytorch_model.bin")


if __name__ == "__main__":
    unittest.main()
