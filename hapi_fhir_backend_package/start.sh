#!/bin/bash

set -e

echo "========================================"
echo "HAPI FHIR Backend"
echo "========================================"

cd "$(dirname "$0")"

echo ""
echo "[1/4] Starting HAPI FHIR..."
docker compose up -d

echo ""
echo "[2/4] Installing Python dependencies..."
python3 -m pip install -r requirements.txt

echo ""
echo "[3/4] Initialising FHIR data..."
python3 setup.py

echo ""
echo "[4/4] Starting FastAPI..."
echo ""
echo "HAPI FHIR:"
echo "http://localhost:8080/fhir"
echo ""
echo "Backend API:"
echo "http://localhost:8000"
echo ""
echo "API Documentation:"
echo "http://localhost:8000/docs"
echo ""

python3 -m uvicorn app:app \
    --host 0.0.0.0 \
    --port 8000
