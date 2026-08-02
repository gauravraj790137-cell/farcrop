import os
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()

BASE_DIR = Path(__file__).resolve().parent.parent          # farcrop/backend/
PROJECT_ROOT = BASE_DIR.parent                              # farcrop/

# Gatekeeper — YOLO leaf-detection model
# The model lives at farcrop/gatekeeper/best.pt (project root level, NOT inside backend/).
GATEKEEPER_MODEL_PATH = Path(
    os.getenv("GATEKEEPER_MODEL_PATH", str(PROJECT_ROOT / "gatekeeper" / "best.pt"))
)
UPLOAD_DIR = os.getenv("UPLOAD_DIR", str(BASE_DIR / "uploads"))
MAX_IMAGE_SIZE = int(os.getenv("MAX_IMAGE_SIZE", str(5 * 1024 * 1024)))
HOST = os.getenv("HOST", "0.0.0.0")
PORT = int(os.getenv("PORT", "8000"))
DEBUG = os.getenv("DEBUG", "True").lower() in {"1", "true", "yes", "on"}
GROQ_API_KEY = os.getenv("GROQ_API_KEY") or os.getenv("groq_api", "")
GROQ_MODEL = os.getenv("GROQ_MODEL", "openai/gpt-oss-120b")
LLM_TIMEOUT_SECONDS = int(os.getenv("LLM_TIMEOUT_SECONDS", "30"))
SERPAPI_API_KEY = os.getenv("SERPAPI_API_KEY") or os.getenv("serp_api", "")
SERPAPI_LOCATION = os.getenv("SERPAPI_LOCATION", "India")
SERPAPI_LANGUAGE = os.getenv("SERPAPI_LANGUAGE", "en")
SERPAPI_COUNTRY = os.getenv("SERPAPI_COUNTRY", "in")
