import logging
import sys
from pathlib import Path

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .models.schemas import StandardResponse

# Make backend/ the import root so `import gatekeeper` resolves to
# backend/gatekeeper/ regardless of how uvicorn is invoked.
_backend_root = str(Path(__file__).resolve().parents[1])
if _backend_root not in sys.path:
    sys.path.insert(0, _backend_root)

from gatekeeper import leaf_detector  # noqa: E402 — must follow sys.path patch

from .api.routes import router
from .config import GATEKEEPER_MODEL_PATH
from .services.inference import crop_disease_model

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("farcrop")

app = FastAPI(
    title="FarCrop Backend",
    description="AI crop disease detection backend for FarCrop.",
    version="2.0.0",
)

app.include_router(router)


@app.on_event("startup")
async def startup_event() -> None:
    """Load both the Gatekeeper and the disease model at startup."""

    # ── 1. Gatekeeper (YOLO) ──────────────────────────────────────────────
    logger.info("═══════════════════════════════════════════════════════════")
    logger.info("[STARTUP] Gatekeeper model path resolved to: %s", GATEKEEPER_MODEL_PATH)
    logger.info("[STARTUP] File exists: %s", GATEKEEPER_MODEL_PATH.exists())
    try:
        leaf_detector.load(model_path=GATEKEEPER_MODEL_PATH)
    except FileNotFoundError as exc:
        logger.critical(
            "[STARTUP] ❌ GATEKEEPER MODEL NOT FOUND — ALL REQUESTS WILL BE REJECTED.\n"
            "  Expected path: %s\n"
            "  Place best.pt at that location and restart.\n"
            "  Error: %s",
            GATEKEEPER_MODEL_PATH, exc
        )
    except Exception as exc:
        logger.critical("[STARTUP] ❌ Gatekeeper failed to load: %s", exc, exc_info=True)
    else:
        logger.info("[STARTUP] ✅ Gatekeeper ready")
    logger.info("═══════════════════════════════════════════════════════════")

    # ── 2. Disease classifier (Hugging Face) ──────────────────────────────
    logger.info("Loading Crop Disease Model…")
    try:
        crop_disease_model.load_model()
    except Exception as exc:
        logger.exception("Disease model load failed: %s", exc)
    else:
        logger.info("Disease model ready")


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    detail = exc.detail if isinstance(exc.detail, str) else "Request failed."
    body = StandardResponse.build_failure(
        stage="request",
        code="REQUEST_FAILED",
        message=detail,
        data={"status_code": exc.status_code},
    )
    return JSONResponse(status_code=exc.status_code, content=body.model_dump())


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
    body = StandardResponse.build_failure(
        stage="validation",
        code="VALIDATION_ERROR",
        message="Invalid request payload.",
        data={"details": exc.errors()},
    )
    return JSONResponse(status_code=422, content=body.model_dump())


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception) -> JSONResponse:
    body = StandardResponse.build_failure(
        stage="server",
        code="INTERNAL_ERROR",
        message="Internal server error.",
    )
    return JSONResponse(status_code=500, content=body.model_dump())
