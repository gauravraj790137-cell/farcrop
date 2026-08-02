from __future__ import annotations

from datetime import date, datetime
from typing import Any

from pydantic import BaseModel, Field, field_validator


class RootResponse(BaseModel):
    """Response model for the health status endpoint."""

    status: str = Field(..., example="running")
    service: str = Field(..., example="FarCrop Backend")


class HealthResponse(BaseModel):
    """Response model for the health endpoint."""

    status: str = Field(..., example="healthy")


class CropCycleTreatment(BaseModel):
    name: str
    date: date


class CropCycleMetadata(BaseModel):
    cycle_id: str
    cycle_name: str
    crop_name: str
    plantation_date: date
    estimated_harvest_date: date
    photo_timestamp: str
    latitude: float | None = None
    longitude: float | None = None
    notes: str | None = None
    treatments: list[CropCycleTreatment] = Field(default_factory=list)

    @field_validator("cycle_id")
    @classmethod
    def validate_cycle_id(cls, value: str) -> str:
        if not value:
            raise ValueError("cycle_id is required")
        return value


class PredictionPayload(BaseModel):
    prediction: dict[str, Any]
    crop_cycle: dict[str, Any]
    generated_at: str


class StandardResponse(BaseModel):
    success: bool
    api_version: str = "2.0"
    stage: str
    code: str
    message: str
    data: dict[str, Any] = Field(default_factory=dict)

    @classmethod
    def build_success(cls, *, stage: str, code: str, message: str, data: dict[str, Any]) -> "StandardResponse":
        return cls(success=True, stage=stage, code=code, message=message, data=data)

    @classmethod
    def build_failure(cls, *, stage: str, code: str, message: str, data: dict[str, Any] | None = None) -> "StandardResponse":
        return cls(success=False, stage=stage, code=code, message=message, data=data or {})


class ErrorResponse(BaseModel):
    """Response model for API errors."""

    message: str


class GatekeeperRejectionResponse(BaseModel):
    """Legacy rejection response for the old route contract."""

    success: bool = False
    stage: str = "gatekeeper"
    reason: str
    message: str


class PredictionEnvelope(BaseModel):
    success: bool = True
    api_version: str = "2.0"
    stage: str = "prediction"
    code: str = "SUCCESS"
    message: str = "Prediction completed."
    data: PredictionPayload
