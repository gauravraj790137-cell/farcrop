import sys
import unittest
from datetime import date
from pathlib import Path

from fastapi import UploadFile

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.models.schemas import CropCycleMetadata, CropCycleTreatment, PredictionEnvelope, PredictionPayload, StandardResponse


class PredictionContractTests(unittest.TestCase):
    def test_crop_cycle_metadata_parses_treatments(self) -> None:
        payload = CropCycleMetadata(
            cycle_id="123e4567-e89b-12d3-a456-426614174000",
            cycle_name="Tomato Field A",
            crop_name="Tomato",
            plantation_date=date(2026, 7, 1),
            estimated_harvest_date=date(2026, 10, 15),
            photo_timestamp="2026-08-02T10:30:00Z",
            latitude=12.34,
            longitude=56.78,
            notes="Healthy leaves observed",
            treatments=[
                CropCycleTreatment(name="Copper Fungicide", date=date(2026, 8, 1)),
                CropCycleTreatment(name="Neem Oil", date=date(2026, 8, 4)),
            ],
        )
        self.assertEqual(payload.treatments[0].name, "Copper Fungicide")
        self.assertEqual(payload.treatments[1].date, date(2026, 8, 4))

    def test_prediction_envelope_builds_shared_response_contract(self) -> None:
        response = StandardResponse.build_success(
            stage="prediction",
            code="SUCCESS",
            message="Prediction completed.",
            data={"prediction": {"disease": "Tomato___Early_blight", "confidence": 92.1}},
        )
        self.assertTrue(response.success)
        self.assertEqual(response.api_version, "2.0")
        self.assertEqual(response.stage, "prediction")
        self.assertEqual(response.code, "SUCCESS")
        self.assertEqual(response.data["prediction"]["disease"], "Tomato___Early_blight")

    def test_prediction_payload_can_hold_cycle_metadata(self) -> None:
        payload = PredictionPayload(
            prediction={"disease": "Tomato___Early_blight", "confidence": 92.1},
            crop_cycle={"cycle_id": "123e4567-e89b-12d3-a456-426614174000", "crop_name": "Tomato"},
            generated_at="2026-08-02T10:30:00Z",
        )
        self.assertEqual(payload.prediction["disease"], "Tomato___Early_blight")
        self.assertEqual(payload.crop_cycle["crop_name"], "Tomato")


if __name__ == "__main__":
    unittest.main()
