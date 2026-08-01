import logging

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from .api.routes import router
from .services.inference import crop_disease_model

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("farcrop")

app = FastAPI(
    title="FarCrop Backend",
    description="AI crop disease detection backend for FarCrop.",
    version="1.0.0",
)

app.include_router(router)


@app.on_event("startup")
async def startup_event() -> None:
    """Load the crop disease model once when the application starts."""
    logger.info("Loading Crop Disease Model...")
    try:
        crop_disease_model.load_model()
    except Exception as exc:  # pragma: no cover - runtime environment path
        logger.exception("Model load failed: %s", exc)
    else:
        logger.info("Model Loaded Successfully")
        logger.info("Inference Ready")


@app.exception_handler(HTTPException)
async def http_exception_handler(_: Request, exc: HTTPException) -> JSONResponse:
    """Return a consistent JSON error payload for HTTP exceptions."""
    return JSONResponse(status_code=exc.status_code, content={"message": exc.detail})


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
    """Handle invalid request payloads and missing parameters."""
    return JSONResponse(status_code=422, content={"message": "Invalid request payload."})


@app.exception_handler(Exception)
async def unhandled_exception_handler(_: Request, exc: Exception) -> JSONResponse:
    """Handle unexpected server errors gracefully."""
    return JSONResponse(status_code=500, content={"message": "Internal server error."})
