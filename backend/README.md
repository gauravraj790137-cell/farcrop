# FarCrop Backend

This directory contains the FastAPI backend for the FarCrop crop disease detection service.

The backend is intentionally structured so the placeholder inference function can later be replaced with a real model without changing the route layer.

## Features

- REST endpoints for health checks and image prediction
- Modular backend structure for future AI model integration
- Placeholder inference service isolated from the API layer
- Image validation and temporary file handling

## Installation

1. Create and activate a virtual environment:

   ```bash
   python -m venv .venv
   .venv\Scripts\activate
   ```

2. Install dependencies:

   ```bash
   pip install -r requirements.txt
   ```

3. Copy the environment example file and adjust values if needed:

   ```bash
   copy .env.example .env
   ```

## Run the server

```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
```

## Example requests

### Root endpoint

```bash
curl http://localhost:8000/
```

### Health endpoint

```bash
curl http://localhost:8000/health
```

### Prediction endpoint

```bash
curl -X POST http://localhost:8000/predict -F "image=@/path/to/image.jpg"
```

## Example response

```json
{
  "disease": "Tomato Early Blight",
  "confidence": 96.42,
  "cause": "Fungal",
  "description": "A common fungal disease that causes dark lesions with concentric rings on tomato leaves.",
  "treatment": [
    "Remove infected leaves",
    "Avoid overhead watering",
    "Spray Mancozeb"
  ],
  "recommended_products": [
    "Mancozeb",
    "Copper Fungicide"
  ]
}
```

## Folder structure

```text
backend/
  app/
    api/
    models/
    services/
    utils/
    config.py
    main.py
  requirements.txt
  README.md
  .env.example
```
