from pydantic import BaseModel, Field


class RootResponse(BaseModel):
    """Response model for the health status endpoint."""

    status: str = Field(..., example="running")
    service: str = Field(..., example="FarCrop Backend")


class HealthResponse(BaseModel):
    """Response model for the health endpoint."""

    status: str = Field(..., example="healthy")


class PredictionResponse(BaseModel):
    """Response model for prediction results."""

    disease: str
    confidence: float
    cause: str
    description: str
    treatment: list[str]
    products: list[str]
    explanation: str
    buy_links: list[dict[str, str]]


class ErrorResponse(BaseModel):
    """Response model for API errors."""

    message: str
