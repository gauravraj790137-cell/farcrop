import json
import sys
from datetime import datetime, timezone
from pathlib import Path

# Ensure backend/ is on the path (mirrors the patch in main.py).
_backend_root = str(Path(__file__).resolve().parents[2])
if _backend_root not in sys.path:
    sys.path.insert(0, _backend_root)

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status
from fastapi.responses import JSONResponse

from ..config import MAX_IMAGE_SIZE, UPLOAD_DIR
from ..models.schemas import (
    ErrorResponse,
    StandardResponse,
)
from ..services.cycle_parser import CropCycleParserService
from ..services.gatekeeper_service import GatekeeperService
from ..services.inference_service import DiseaseInferenceService
from ..services.llm_service import LLMService
from ..services.metadata_service import DiseaseMetadataService
from ..utils.image import delete_temp, save_upload, validate_image

router = APIRouter()

cycle_parser_service = CropCycleParserService()
gatekeeper_service = GatekeeperService()
inference_service = DiseaseInferenceService()
metadata_service = DiseaseMetadataService()
llm_service = LLMService()


@router.get("/", response_model=StandardResponse)
async def root() -> StandardResponse:
    return StandardResponse.build_success(
        stage="health",
        code="SUCCESS",
        message="Service is running.",
        data={"status": "running", "service": "FarCrop Backend"},
    )


@router.get("/health", response_model=StandardResponse)
async def health() -> StandardResponse:
    return StandardResponse.build_success(
        stage="health",
        code="SUCCESS",
        message="Service is healthy.",
        data={"status": "healthy"},
    )


@router.post(
    "/predict",
    responses={
        status.HTTP_200_OK: {"model": StandardResponse},
        status.HTTP_400_BAD_REQUEST: {"model": ErrorResponse},
        status.HTTP_413_REQUEST_ENTITY_TOO_LARGE: {"model": ErrorResponse},
        status.HTTP_422_UNPROCESSABLE_ENTITY: {"model": ErrorResponse},
        status.HTTP_451_UNAVAILABLE_FOR_LEGAL_REASONS: {"model": StandardResponse},
        status.HTTP_500_INTERNAL_SERVER_ERROR: {"model": ErrorResponse},
        status.HTTP_503_SERVICE_UNAVAILABLE: {"model": ErrorResponse},
    },
)
async def predict_disease(
    image: UploadFile | None = File(None, alias="image"),
    cycle_id: str | None = Form(default=None),
    cycle_name: str | None = Form(default=None),
    crop_name: str | None = Form(default=None),
    plantation_date: str | None = Form(default=None),
    estimated_harvest_date: str | None = Form(default=None),
    photo_timestamp: str | None = Form(default=None),
    latitude: float | None = Form(default=None),
    longitude: float | None = Form(default=None),
    notes: str | None = Form(default=None),
    treatments: str | None = Form(default=None),
) -> JSONResponse:
    """Handle v2 prediction requests with crop-cycle metadata and a shared response envelope."""

    if image is None or not image.filename:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Image file is required.")

    try:
        await validate_image(image, MAX_IMAGE_SIZE)
    except ValueError as exc:
        message = str(exc)
        code = status.HTTP_413_REQUEST_ENTITY_TOO_LARGE if "too large" in message.lower() else status.HTTP_400_BAD_REQUEST
        raise HTTPException(status_code=code, detail=message) from exc

    parsed_treatments: list[dict[str, str]] = []
    if treatments:
        try:
            parsed_treatments = json.loads(treatments)
        except json.JSONDecodeError as exc:
            raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail="treatments must be valid JSON") from exc

    cycle_payload = {
        "cycle_id": cycle_id,
        "cycle_name": cycle_name,
        "crop_name": crop_name,
        "plantation_date": plantation_date,
        "estimated_harvest_date": estimated_harvest_date,
        "photo_timestamp": photo_timestamp,
        "latitude": latitude,
        "longitude": longitude,
        "notes": notes,
        "treatments": parsed_treatments,
    }

    try:
        metadata = cycle_parser_service.parse(cycle_payload)
    except Exception as exc:  # pragma: no cover - validation path
        raise HTTPException(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=str(exc)) from exc

    temp_path: str | None = None
    try:
        temp_path = await save_upload(image, UPLOAD_DIR)
        gate = gatekeeper_service.validate_crop(temp_path)
        if not gate.passed:
            if gate.code == "NO_LEAF_DETECTED":
                body = StandardResponse.build_failure(
                    stage="gatekeeper",
                    code="NO_LEAF_DETECTED",
                    message="No crop leaf detected in this image.",
                )
            else:
                # UNSUPPORTED_CROP or any other rejection
                body = StandardResponse.build_failure(
                    stage="gatekeeper",
                    code="UNSUPPORTED_CROP",
                    message="Only Tomato, Potato and Bell Pepper are currently supported.",
                    data={
                        "detected_classes": list(gate.detected_classes) or [gate.crop or "unknown"],
                        "confidence": round(gate.confidence, 4),
                    },
                )
            return JSONResponse(status_code=status.HTTP_451_UNAVAILABLE_FOR_LEGAL_REASONS, content=body.model_dump())

        prediction = inference_service.infer(temp_path)
        enriched = metadata_service.enrich(prediction)
        llm_payload = llm_service.generate(enriched, metadata.model_dump(mode="json"))
        enriched.update(llm_payload)

        generated_at = photo_timestamp or datetime.now(timezone.utc).isoformat()
        return JSONResponse(
            status_code=status.HTTP_200_OK,
            content=StandardResponse.build_success(
                stage="prediction",
                code="SUCCESS",
                message="Prediction completed.",
                data={
                    "prediction": enriched,
                    "crop_cycle": metadata.model_dump(mode="json"),
                    "generated_at": generated_at,
                },
            ).model_dump(),
        )

    except FileNotFoundError as exc:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(exc)) from exc
    except RuntimeError as exc:
        raise HTTPException(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=str(exc)) from exc
    except HTTPException:
        raise
    except Exception as exc:  # pragma: no cover
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail="Unable to process image.") from exc
    finally:
        if temp_path:
            delete_temp(temp_path)
