from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.routes import router

app = FastAPI(
    title="FarCrop Backend",
    description="AI crop disease detection backend for FarCrop.",
    version="1.0.0",
)

app.include_router(router)


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
