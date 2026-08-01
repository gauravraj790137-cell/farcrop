from fastapi import APIRouter, File, HTTPException, UploadFile, status

from ..config import MAX_IMAGE_SIZE, UPLOAD_DIR
from ..models.schemas import ErrorResponse, HealthResponse, PredictionResponse, RootResponse
from ..services.disease_service import enrich_prediction
from ..services.inference import predict
from ..utils.image import delete_temp, save_upload, validate_image

router = APIRouter()


@router.get("/", response_model=RootResponse)
async def root() -> RootResponse:
    """Return basic service metadata."""
    return RootResponse(status="running", service="FarCrop Backend")


@router.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    """Return the health status of the service."""
    return HealthResponse(status="healthy")


@router.post(
    "/predict",
    response_model=PredictionResponse,
    responses={
        status.HTTP_400_BAD_REQUEST: {"model": ErrorResponse},
        status.HTTP_413_REQUEST_ENTITY_TOO_LARGE: {"model": ErrorResponse},
        status.HTTP_422_UNPROCESSABLE_ENTITY: {"model": ErrorResponse},
        status.HTTP_500_INTERNAL_SERVER_ERROR: {"model": ErrorResponse},
        status.HTTP_503_SERVICE_UNAVAILABLE: {"model": ErrorResponse},
    },
)
async def predict_disease(image: UploadFile | None = File(None, alias="image")) -> PredictionResponse:
    """Validate an uploaded image, run inference, and return a disease response."""
    if image is None or not image.filename:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Image file is required.")

    try:
        await validate_image(image, MAX_IMAGE_SIZE)
    except ValueError as exc:
        message = str(exc)
        if "too large" in message.lower():
            raise HTTPException(status_code=status.HTTP_413_REQUEST_ENTITY_TOO_LARGE, detail=message) from exc
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=message) from exc

    temp_path: str | None = None
    try:
        temp_path = await save_upload(image, UPLOAD_DIR)
        prediction = predict(temp_path)
        enriched_prediction = enrich_prediction(prediction)
        return PredictionResponse(**enriched_prediction)
    except FileNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(exc)) from exc
    except HTTPException:
        raise
    except Exception as exc:  # pragma: no cover - defensive guard
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Unable to process image.",
        ) from exc
    finally:
        if temp_path:
            delete_temp(temp_path)
