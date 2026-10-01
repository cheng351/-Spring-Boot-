# HAPI FHIR + SMART on FHIR Environment

This directory contains the local FHIR environment used by MindStep. It
combines the existing HAPI FHIR/PostgreSQL package with the SMART on
FHIR development components required to launch MindStep from an EHR
context.

## What This Package Provides

The current Docker Compose environment contains four containers:

``` text
hapi-postgres
    |
    v
hapi-fhir (:8080)
    ^
    |
mindstep-smart-launcher (:8090)
    ^
    |
mindstep-ehr (:8082)
```

Their roles are:

  -----------------------------------------------------------------------
  Container                           Purpose
  ----------------------------------- -----------------------------------
  `hapi-postgres`                     Persistent PostgreSQL database used
                                      by HAPI FHIR

  `hapi-fhir`                         Local HAPI FHIR R4 server

  `mindstep-smart-launcher`           SMART launcher/proxy configured to
                                      use the local HAPI server

  `mindstep-ehr`                      SMART EHR Launcher UI used to
                                      choose a practitioner/patient and
                                      launch MindStep
  -----------------------------------------------------------------------

## Directory Structure

Important files/directories include:

``` text
hapi_fhir_backend_package/
├── docker-compose.yml
├── README.md
├── app.py
├── fhir_reader.py
├── setup.py
├── start.sh
├── requirements.txt
├── fhir_data/
└── smart-auth/
    ├── docker-compose.smart.yml
    └── SMART-EHR-Launcher/
        ├── Dockerfile
        ├── config.docker.json
        ├── package.json
        └── ...
```

`docker-compose.yml` is now the main unified Compose file. It starts the
FHIR database/server and both SMART components together.

`smart-auth/docker-compose.smart.yml` is the earlier standalone SMART
launcher configuration and is retained as a reference; normal team
startup should use the unified `docker-compose.yml` in this directory.

## Prerequisites

Required for the Docker environment:

-   Docker Desktop
-   Docker Compose (included with current Docker Desktop)

Python 3 and pip are required only when using the package's Python
setup/import/helper scripts.

Check:

``` bash
docker --version
docker compose version
python3 --version
```

## Quick Start

From the repository root:

``` bash
cd hapi_fhir_backend_package
docker compose up -d --build
```

On the first run Docker may need to download the PostgreSQL, HAPI FHIR,
and SMART Launcher images and build the local SMART EHR Launcher image.

Verify:

``` bash
docker compose ps
```

Expected containers:

``` text
hapi-postgres
hapi-fhir
mindstep-smart-launcher
mindstep-ehr
```

Expected ports:

``` text
5432  PostgreSQL
8080  HAPI FHIR
8082  SMART EHR Launcher
8090  SMART Launcher / Proxy
```

## Why `--build`?

The `mindstep-ehr` service is built from:

``` text
smart-auth/SMART-EHR-Launcher/Dockerfile
```

Therefore a teammate does not need a pre-existing
`mindstep-ehr-launcher` image.

This command:

``` bash
docker compose up -d --build
```

will:

1.  Pull required public images when necessary.
2.  Build `mindstep-ehr-launcher` from the repository.
3.  Create the containers.
4.  Start the complete Docker environment.

For later runs, when the EHR Launcher has not changed, this is normally
enough:

``` bash
docker compose up -d
```

## HAPI FHIR

FHIR base URL:

``` text
http://localhost:8080/fhir
```

Capability statement:

``` text
http://localhost:8080/fhir/metadata
```

Patient search:

``` text
http://localhost:8080/fhir/Patient
```

Practitioner search:

``` text
http://localhost:8080/fhir/Practitioner
```

HAPI FHIR stores its data in PostgreSQL through the persistent Docker
volume:

``` text
hapi-postgres-data
```

Stopping/recreating containers does not normally delete the FHIR data
while this volume is retained.

## FHIR Dataset and Import

The package includes project FHIR JSON data under `fhir_data/`. The
existing helper scripts were designed to import and verify this data.

The original package workflow uses:

``` bash
chmod +x start.sh
./start.sh
```

The helper workflow can:

1.  Start/check HAPI FHIR.
2.  Wait for the FHIR server.
3.  Check whether patient data already exists.
4.  Import the FHIR transaction Bundles when required.
5.  Verify FHIR resources.
6.  Start the package's FastAPI helper backend.

For the current MindStep application, the main application backend is
Spring Boot on port 8081. The Python/FastAPI files are retained as
package utilities/legacy integration components and are not the Spring
Boot application itself.

### Important: fresh teammate database

Docker images do not contain your existing local PostgreSQL patient
records. A teammate starting with a fresh Docker volume must import the
project FHIR dataset before expecting the same patients/resources that
exist on your machine.

Do not assume that cloning the repository automatically copies your
Docker database.

## SMART on FHIR Components

### SMART Launcher / Proxy

Container:

``` text
mindstep-smart-launcher
```

Local endpoint:

``` text
http://localhost:8090
```

The Compose configuration connects it to the local HAPI FHIR server
using:

``` text
FHIR_SERVER_R4=http://host.docker.internal:8080/fhir
```

SMART configuration endpoint:

``` text
http://localhost:8090/v/r4/fhir/.well-known/smart-configuration
```

### SMART EHR Launcher

Container:

``` text
mindstep-ehr
```

Open:

``` text
http://localhost:8082
```

The EHR Launcher is built from the source under:

``` text
smart-auth/SMART-EHR-Launcher/
```

It is configured to use the SMART-enabled FHIR endpoint rather than a
public demonstration FHIR server.

## How to Use SMART Launch

Before testing, ensure:

-   the four Docker containers are running;
-   the Spring Boot backend is running on port 8081;
-   the MindStep frontend is running on port 3000;
-   the required Patient and Practitioner resources exist in HAPI FHIR.

Then:

1.  Open `http://localhost:8082`.
2.  Select a Practitioner.
3.  Select a Patient.
4.  Click **Launch MindStep**.
5.  The SMART launch flow opens MindStep.
6.  MindStep obtains the selected patient's SMART patient context.
7.  MindStep uses that patient ID to load the corresponding record.

The current test environment includes a Practitioner used for SMART
demonstration and a test patient named Jennifer. These are development
records, not a limitation of SMART.

## Adding More Patients

SMART can work with other patients. The important requirement is that
the patient's FHIR resources exist in the local HAPI FHIR server.

FHIR Patient resources can be imported as part of the project dataset or
created through the HAPI FHIR REST API.

Example structure:

``` json
{
  "resourceType": "Patient",
  "name": [
    {
      "family": "Example",
      "given": ["Patient"]
    }
  ],
  "gender": "female"
}
```

A Patient alone may not provide enough information for the MindStep UI.
If the application expects Conditions, Observations, Encounters,
MedicationRequests, or other clinical resources, those resources must
also reference the same patient.

After adding patients, verify them with:

``` bash
curl -s "http://localhost:8080/fhir/Patient"
```

## Adding a Practitioner

The SMART EHR Launcher can use Practitioner resources stored in HAPI
FHIR.

Example:

``` bash
curl -X POST \
  http://localhost:8080/fhir/Practitioner \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Practitioner",
    "active": true,
    "name": [
      {
        "use": "official",
        "family": "Surgeon",
        "given": ["David"],
        "prefix": ["Dr."]
      }
    ]
  }'
```

Verify:

``` bash
curl -s "http://localhost:8080/fhir/Practitioner"
```

## Direct FHIR Verification

Check Patient count:

``` bash
curl -s "http://localhost:8080/fhir/Patient?_summary=count"
```

Check Conditions:

``` bash
curl -s "http://localhost:8080/fhir/Condition?_summary=count"
```

Check Observations:

``` bash
curl -s "http://localhost:8080/fhir/Observation?_summary=count"
```

Check SMART configuration:

``` bash
curl -s "http://localhost:8090/v/r4/fhir/.well-known/smart-configuration"
```

## Docker Management

Show the current environment:

``` bash
docker compose ps
```

View logs:

``` bash
docker compose logs
```

Follow logs:

``` bash
docker compose logs -f
```

Restart:

``` bash
docker compose restart
```

Stop/remove containers while retaining the database volume:

``` bash
docker compose down
```

Start again:

``` bash
docker compose up -d
```

Rebuild the EHR Launcher after changing its source/configuration:

``` bash
docker compose up -d --build
```

## Persistent Data Warning

This command retains the PostgreSQL volume:

``` bash
docker compose down
```

This command removes volumes and can delete the stored local FHIR
database:

``` bash
docker compose down -v
```

Do not use `-v` unless you intentionally want a fresh database and are
prepared to re-import the FHIR dataset.

## Common Problems

### EHR Launcher opens but has no patients

Check:

``` text
http://localhost:8080/fhir/Patient
```

If the database is fresh, import the project FHIR dataset.

### EHR Launcher has no practitioner

Check:

``` text
http://localhost:8080/fhir/Practitioner
```

Create/import a Practitioner resource if necessary.

### SMART proxy is unavailable

Check:

``` bash
docker compose ps
docker compose logs smart-launcher
```

Then test:

``` text
http://localhost:8090/v/r4/fhir/.well-known/smart-configuration
```

### EHR Launcher is unavailable

Check:

``` bash
docker compose logs mindstep-ehr
```

Then open:

``` text
http://localhost:8082
```

### Container name or port conflict

Inspect existing containers:

``` bash
docker ps -a
```

Check the relevant port on macOS:

``` bash
lsof -i :8080
lsof -i :8082
lsof -i :8090
```

Avoid deleting Docker volumes unless you know the stored FHIR data can
be recreated.

## Relationship to the Main MindStep Application

The Docker environment does not start the Spring Boot application or
Next.js frontend.

Those remain separate development processes:

``` text
Spring Boot backend  -> http://localhost:8081
MindStep frontend    -> http://localhost:3000
```

The complete SMART workflow is therefore:

``` text
SMART EHR Launcher (:8082)
        ↓
SMART Launcher / Proxy (:8090)
        ↓
MindStep (:3000)
        ↓
Spring Boot (:8081)
        ↓
HAPI FHIR (:8080)
        ↓
PostgreSQL
```

Gemini is called by the application backend for AI-generated
plain-language guidance.

## Team Setup Summary

For a new teammate:

``` bash
git clone <repository-url>
cd 2026-HighFive/hapi_fhir_backend_package
docker compose up -d --build
```

Then ensure the FHIR dataset is imported, start the Spring Boot backend
and frontend according to the root `README.md`, and open:

``` text
http://localhost:8082
```

The teammate does not need your local Docker containers or locally built
images. The repository plus Docker Compose/Dockerfile is sufficient to
recreate the Docker environment; the FHIR database content must still be
imported on a fresh volume.
