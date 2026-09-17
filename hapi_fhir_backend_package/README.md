# HAPI FHIR Backend Package

This package provides a local HAPI FHIR R4 server, the project FHIR dataset, a Python FHIR reader, and a FastAPI backend interface.

The package is designed so that a developer can start the complete local backend environment with one command.

## 1. Package Structure

```text
hapi_fhir_backend_package/
├── docker-compose.yml
├── app.py
├── fhir_reader.py
├── setup.py
├── start.sh
├── requirements.txt
├── README.md
└── fhir_data/
    ├── patient_001_....json
    ├── patient_002_....json
    ├── ...
    └── patient_050_....json
```

### Main Files

- `docker-compose.yml` — starts HAPI FHIR and its database using Docker.
- `fhir_data/` — contains the 50 patient FHIR JSON files.
- `setup.py` — waits for HAPI FHIR, checks whether patient data already exists, imports the JSON files when necessary, and verifies the imported resources.
- `fhir_reader.py` — retrieves Patient, Condition, Observation, Encounter, and MedicationRequest resources from HAPI FHIR.
- `app.py` — exposes backend API endpoints using FastAPI.
- `start.sh` — starts the complete local backend environment.
- `requirements.txt` — Python dependencies.

## 2. Prerequisites

Install the following before starting:

- Docker Desktop
- Python 3
- pip

Make sure Docker Desktop is running before executing the startup script.

You can check Docker with:

```bash
docker --version
docker compose version
```

You can check Python with:

```bash
python3 --version
```

## 3. First-Time Setup

Open Terminal and enter the project directory:

```bash
cd hapi_fhir_backend_package
```

Give the startup script permission to run:

```bash
chmod +x start.sh
```

Then start the complete backend:

```bash
./start.sh
```

The startup script will automatically:

1. Start HAPI FHIR and its database with Docker.
2. Install/check the required Python packages.
3. Wait until HAPI FHIR is ready.
4. Check whether FHIR patient data already exists.
5. Import the JSON files from `fhir_data/` if the database is empty.
6. Verify the main FHIR resource counts.
7. Start the FastAPI backend.

The initial startup can take longer because Docker may need to download images and HAPI FHIR needs time to initialise.

## 4. FHIR Data Import

The files in `fhir_data/` are FHIR transaction Bundles.

They are sent to the HAPI FHIR base endpoint:

```text
http://localhost:8080/fhir
```

HAPI FHIR processes each transaction Bundle and stores the resources contained inside it.

The import is normally required only when the HAPI FHIR database is empty.

If Patient resources already exist, `setup.py` skips the import to avoid importing the same dataset again.

After a successful import, the verification output should show the expected resource counts for the dataset, including 50 Patient resources.

## 5. Local Services

After startup, the following local services are available.

### HAPI FHIR Server

```text
http://localhost:8080/fhir
```

FHIR capability statement:

```text
http://localhost:8080/fhir/metadata
```

Example Patient search:

```text
http://localhost:8080/fhir/Patient
```

### FastAPI Backend

```text
http://localhost:8000
```

Interactive API documentation:

```text
http://localhost:8000/docs
```

Use `/docs` to inspect and test the endpoints implemented in `app.py`.

## 6. Architecture

The data flow is:

```text
FHIR JSON files
      ↓
setup.py
      ↓
HAPI FHIR R4 Server
      ↓
FHIR REST API
      ↓
fhir_reader.py
      ↓
FastAPI (app.py)
      ↓
Frontend / Backend processing
```

HAPI FHIR is the FHIR data server. The Python code does not directly read the original JSON files during normal application use. Instead, it requests the stored resources from HAPI FHIR through its REST API.

For example:

```text
GET /fhir/Patient/{id}
GET /fhir/Condition?patient={id}
GET /fhir/Observation?patient={id}
GET /fhir/Encounter?patient={id}
GET /fhir/MedicationRequest?patient={id}
```

This keeps the application layer separated from the FHIR storage layer.

## 7. Starting the Project Later

After the first successful setup, normally run:

```bash
./start.sh
```

If the Docker database volume still exists, the FHIR data remains stored and `setup.py` will detect the existing patients and skip re-importing the JSON files.

## 8. Stopping the Backend

Stop FastAPI in the Terminal with:

```text
Control + C
```

To stop the Docker services:

```bash
docker compose down
```

This normally stops and removes the containers while retaining persistent Docker volume data.

Do not use the following command unless you intentionally want to remove the persistent database data:

```bash
docker compose down -v
```

Deleting the Docker volume can remove the stored FHIR data and require the JSON dataset to be imported again.

## 9. Manual Verification

Check running Docker containers:

```bash
docker ps
```

Check the number of Patient resources:

```bash
curl -s "http://localhost:8080/fhir/Patient?_summary=count"
```

Check Observation resources:

```bash
curl -s "http://localhost:8080/fhir/Observation?_summary=count"
```

Check Condition resources:

```bash
curl -s "http://localhost:8080/fhir/Condition?_summary=count"
```

You can also test the reader directly:

```bash
python3 fhir_reader.py
```

## 10. Common Problems

### Docker container name conflict

If Docker reports that a container name such as `hapi-postgres` is already in use, another HAPI FHIR environment may already exist on the computer.

Check containers:

```bash
docker ps -a
```

Do not delete an existing container or volume unless you know it is safe to do so.

### HAPI FHIR is not ready

HAPI FHIR may require some time to initialise, especially on the first run. `setup.py` automatically waits for the server before importing data.

You can manually test:

```bash
curl http://localhost:8080/fhir/metadata
```

### HTTP 422 when importing a transaction Bundle

FHIR transaction Bundles must be POSTed to the FHIR base endpoint:

```text
http://localhost:8080/fhir
```

They should not be POSTed to:

```text
http://localhost:8080/fhir/Bundle
```

The provided `setup.py` handles this automatically.

### Port already in use

The project normally uses:

```text
8080 — HAPI FHIR
8000 — FastAPI
```

Check port 8080 on macOS with:

```bash
lsof -i :8080
```

Check port 8000 with:

```bash
lsof -i :8000
```

## 11. Development Notes

The current package is intended for local development and team integration.

The backend developer can use the FastAPI layer for application-specific processing while HAPI FHIR remains responsible for storing and serving FHIR R4 resources.

The frontend should normally communicate with the application backend rather than depending directly on the raw JSON files.

## Quick Start

For an already prepared machine:

```bash
cd hapi_fhir_backend_package
chmod +x start.sh
./start.sh
```

Then open:

```text
FastAPI documentation:
http://localhost:8000/docs

HAPI FHIR:
http://localhost:8080/fhir
```
