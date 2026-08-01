import os
from io import BytesIO
from pathlib import Path

from fastapi import UploadFile
from PIL import Image, UnidentifiedImageError


async def validate_image(file: UploadFile, max_size: int) -> None:
    """Validate uploaded image presence, type and size."""
    if not file.filename:
        raise ValueError("Image file is required.")

    if file.content_type not in {"image/jpeg", "image/png", "image/webp", "image/jpg"}:
        raise ValueError("Unsupported file type. Please upload a JPEG, PNG, or WebP image.")

    contents = await file.read()
    if not contents:
        raise ValueError("Invalid image file.")

    if len(contents) > max_size:
        raise ValueError(f"Image size exceeds the maximum limit of {max_size // (1024 * 1024)}MB.")

    try:
        image = Image.open(BytesIO(contents))
        image.verify()
    except (UnidentifiedImageError, OSError) as exc:
        raise ValueError("Corrupt or invalid image file.") from exc

    await file.seek(0)


async def save_upload(file: UploadFile, upload_dir: str) -> str:
    """Persist an uploaded image to disk and return the saved path."""
    Path(upload_dir).mkdir(parents=True, exist_ok=True)

    file_path = Path(upload_dir) / f"{Path(file.filename or 'upload').stem}_{os.urandom(8).hex()}"
    if file.filename and Path(file.filename).suffix.lower() in {".jpg", ".jpeg", ".png", ".webp"}:
        file_path = file_path.with_suffix(Path(file.filename).suffix.lower())

    contents = await file.read()
    with file_path.open("wb") as handle:
        handle.write(contents)

    return str(file_path)


def delete_temp(path: str) -> None:
    """Delete a temporary file if it exists."""
    try:
        os.remove(path)
    except FileNotFoundError:
        return
