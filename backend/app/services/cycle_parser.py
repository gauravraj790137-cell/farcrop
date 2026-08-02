from __future__ import annotations

from datetime import date, datetime
from typing import Any

from ..models.schemas import CropCycleMetadata


class CropCycleParserService:
    """Parse form-data values into a validated crop-cycle metadata object."""

    def parse(self, payload: dict[str, Any]) -> CropCycleMetadata:
        return CropCycleMetadata(**payload)

    def to_payload(self, metadata: CropCycleMetadata) -> dict[str, Any]:
        return metadata.model_dump(mode="json")
