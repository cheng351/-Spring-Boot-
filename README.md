# 2026-HighFive --- MindStep

MindStep is a patient-facing health support application built around
FHIR health records. The current development setup combines a web
frontend, a Spring Boot application backend, a local HAPI FHIR R4
server, Gemini-based plain-language guidance, and SMART on FHIR launch
support.

## System Architecture

``` text
Staff / Clinician flow:
SMART EHR Launcher (:8082)
        |
        | select Practitioner + Patient
        v
SMART Launcher / Proxy (:8090)
        |
        | SMART launch + patient context
        v
MindStep Frontend (:3000)

Patient flow:
MindStep Patient Launch (:3000/patient-launch)
        |
        v
Custom SMART Launcher (:8090)
        |
        | select local Patient + simulated login
        v
SMART Callback (:3000/smart-callback)
        |
        v
MindStep Frontend (:3000)
        |
        v
Spring Boot Backend (:8081)
       / \
      /   \
HAPI FHIR  Gemini
 (:8080)
    |
PostgreSQL (:5432)
```

MindStep can also be opened directly at `http://localhost:3000` for
development. A direct launch does not receive a patient context from an
EHR and therefore uses the application's configured fallback patient.

For detailed FHIR and SMART on FHIR setup, see
`hapi_fhir_backend_package/README.md`.

## Prerequisites

Install these before running the complete project:

-   Git
-   Docker Desktop
-   Java 17
-   Node.js 22.13 or later
-   npm
-   A Gemini API key if the backend configuration uses `GEMINI_API_KEY`
-   Python 3 only if you need to use the legacy/helper Python scripts
    inside `hapi_fhir_backend_package`

Check the main tools with:

``` bash
git --version
docker --version
docker compose version
java -version
node -v
npm -v
```

Docker Desktop must be running before starting the FHIR/SMART
environment.

> **Node.js:** use Node 22.13+ for this frontend. Older versions such as
> Node 20 may fail because required Node APIs are unavailable.

## 1. Clone the Project

``` bash
git clone <repository-url>
cd 2026-HighFive
```

## 2. Start the FHIR + SMART Docker Environment

The unified Docker Compose configuration creates the local database,
FHIR server, custom SMART launcher/proxy, and SMART EHR Launcher. The
custom SMART launcher is built from source stored in this repository, so
teammates do not need to manually create or copy an additional container.

``` bash
cd hapi_fhir_backend_package
docker compose up -d --build
```

This starts:

  ------------------------------------------------------------------------
  Service                                 Local port Purpose
  --------------------- ---------------------------- ---------------------
  PostgreSQL                                    5432 Persistent HAPI FHIR
                                                     database

  HAPI FHIR R4                                  8080 FHIR data server

  Custom SMART Launcher /                       8090 SMART-enabled proxy
  Proxy                                              to the local FHIR
                                                     server; also supports
                                                     the patient login demo

  SMART EHR Launcher                            8082 Simulated EHR used to
                                                     select a patient and
                                                     launch MindStep
  ------------------------------------------------------------------------

Verify the containers:

``` bash
docker compose ps
```

The expected containers are:

``` text
hapi-postgres
hapi-fhir
mindstep-smart-launcher
mindstep-ehr
```

A teammate does **not** need the `mindstep-ehr-launcher` or custom
SMART launcher image in advance. `docker compose up -d --build` builds
the repository-owned Docker images automatically from their Dockerfiles.
Public Docker images are pulled automatically when required.

## 3. FHIR Test Data

The HAPI package contains the project FHIR dataset and helper scripts
for importing/verifying it. On a fresh Docker volume, ensure the FHIR
data has been imported before testing MindStep.

Detailed import and verification instructions are in:

``` text
hapi_fhir_backend_package/README.md
```

The current SMART demonstration uses a test patient named Jennifer.
SMART is not limited to Jennifer: additional Patient resources can be
stored in HAPI FHIR and selected through the EHR Launcher when
available.

## 4. Configure Gemini

Do not commit a real API key to Git.

Recommended Spring configuration:

``` yaml
ai:
  provider:
    api-key: "${GEMINI_API_KEY}"
```

Before starting the backend:

``` bash
export GEMINI_API_KEY="YOUR_API_KEY"
```

If your local `application-dev.yml` currently contains a hard-coded key,
replace it with the environment-variable form before pushing to a shared
or public repository. Any key exposed in screenshots, chat, commits, or
a repository should be revoked and replaced.

## 5. Start the Spring Boot Backend

From the project root:

``` bash
cd ..
./mvnw spring-boot:run
```

The application backend runs on:

``` text
http://localhost:8081
```

Keep this terminal open.

## 6. Start the Frontend

Open another terminal:

``` bash
cd 2026-HighFive/frontend
npm install
npm run dev
```

The frontend runs on:

``` text
http://localhost:3000
```

`fhirclient` is a project dependency, so teammates should receive it
through `npm install`; they do not need to install it globally.

## 7. Run MindStep Through SMART on FHIR

MindStep now supports two local SMART demonstration flows.

### 7.1 Staff / Clinician EHR Launch

Open:

``` text
http://localhost:8082
```

Then:

1.  Select a Practitioner.
2.  Select a Patient.
3.  Choose **Launch MindStep**.
4.  The EHR Launcher starts the SMART flow.
5.  MindStep receives the selected patient context.
6.  The frontend stores the SMART patient ID for the session.
7.  MindStep uses that patient ID when requesting the patient's
    information.

### 7.2 Patient Login / Patient-Facing Launch

Open:

``` text
http://localhost:3000/patient-launch
```

The patient-facing route starts a SMART authorization flow through the
custom SMART launcher on port 8090. The local patient login screen can
select a Patient resource from the local HAPI FHIR dataset. After the
simulated login/authorization completes, SMART redirects to:

``` text
http://localhost:3000/smart-callback
```

MindStep then uses the returned local patient ID. For example, the
current demo can sign in as Jennifer Johnson and load her local FHIR
record successfully.

> **Important:** the patient login is a local prototype/simulation for
> development and demonstration. It is not a production authentication
> system and must not be treated as real patient identity verification.

The SMART integration is implemented through `fhirclient`, the SMART
launch/callback pages, and the custom local SMART launcher.

Important frontend routes:

``` text
/smart-launch
/patient-launch
/smart-callback
```

The application page uses the SMART patient ID when present and falls
back to the configured development patient when there is no SMART
context.

## 8. Direct Launch vs SMART Launch

### Direct development launch

Open:

``` text
http://localhost:3000
```

There is no EHR launch context, so the application uses its fallback
patient ID. This mode is useful for frontend/backend development.

### SMART staff launch

Open the EHR Launcher:

``` text
http://localhost:8082
```

Select the practitioner and patient and launch MindStep. The selected
EHR patient context is passed to MindStep.

### SMART patient launch

Open:

``` text
http://localhost:3000/patient-launch
```

The custom SMART launcher presents the local patient-facing login flow
and returns the selected patient's context to MindStep.

In both SMART modes, the application receives patient context instead
of requiring the user to manually type or hard-code a patient ID.

## 9. Important Local URLs

  -------------------------------------------------------------------------------------------------------
  Component                           URL
  ----------------------------------- -------------------------------------------------------------------
  MindStep frontend                   `http://localhost:3000`

  Spring Boot backend                 `http://localhost:8081`

  HAPI FHIR base                      `http://localhost:8080/fhir`

  HAPI capability statement           `http://localhost:8080/fhir/metadata`

  SMART EHR Launcher                  `http://localhost:8082`

  Custom SMART launcher / proxy       `http://localhost:8090`

  Patient-facing launch                `http://localhost:3000/patient-launch`

  SMART callback                       `http://localhost:3000/smart-callback`

  SMART configuration                  `http://localhost:8090/v/r4/fhir/.well-known/smart-configuration`
  -------------------------------------------------------------------------------------------------------

## 10. Normal Startup Order

After the first setup, the recommended order is:

``` text
1. Start Docker Desktop
2. docker compose up -d --build
3. Start Spring Boot (:8081)
4. Start the frontend (:3000)
5. For staff flow: open SMART EHR Launcher (:8082)
6. For patient flow: open MindStep /patient-launch (:3000/patient-launch)
```

For normal day-to-day use, `--build` is not always necessary:

``` bash
docker compose up -d
```

Use `--build` when the SMART EHR Launcher, custom SMART launcher, or
Docker configuration has changed, and for a teammate's first setup.

## 11. Stop the Project

Stop the frontend and Spring Boot processes with `Control + C` in their
terminals.

Stop the Docker environment:

``` bash
cd hapi_fhir_backend_package
docker compose down
```

This retains the PostgreSQL Docker volume.

Do **not** use this unless you intentionally want to remove the stored
FHIR database:

``` bash
docker compose down -v
```

## 12. Common Problems

### Docker is not running

Start Docker Desktop and retry:

``` bash
docker compose up -d --build
```

### Port already in use

The development setup expects ports 3000, 5432, 8080, 8081, 8082, and
8090.

On macOS, for example:

``` bash
lsof -i :8080
lsof -i :8081
lsof -i :8082
lsof -i :8090
```

### Frontend fails under an old Node version

Check:

``` bash
node -v
```

Use Node 22.13 or later.

### MindStep loads but the AI assistant is unavailable

First verify that the Spring backend is reachable and that its Gemini
configuration/API key is valid. The health record can continue to work
even when the language-model request fails.

### SMART EHR Launcher works but no patients/practitioners appear

Verify that HAPI FHIR contains the required `Patient` and `Practitioner`
resources and that the SMART proxy is pointing to the local HAPI FHIR
server.

### Patient login opens but SMART authorization fails

Make sure the custom `smart-launcher` image was rebuilt after pulling
the latest project changes:

``` bash
cd hapi_fhir_backend_package
docker compose up -d --build smart-launcher
```

Also make sure the frontend is running on port 3000 because the patient
SMART flow redirects back to `/smart-callback`.

### Patient login succeeds but the health record is unavailable

The patient context returned by SMART must refer to a Patient resource
that exists in the same local HAPI FHIR server used by MindStep. Use a
patient from the local patient list rather than an unrelated external
SMART test dataset.

### Teammate does not have the SMART Docker images

They should not copy containers from another computer. From
`hapi_fhir_backend_package`, run:

``` bash
docker compose up -d --build
```

Docker pulls required public base images and builds the local EHR
Launcher and custom SMART launcher from the repository.

## 13. Security Notes

-   Never commit Gemini API keys or other credentials.
-   Use environment variables for secrets.
-   Revoke keys that have been exposed.
-   The current Docker/FHIR/SMART configuration is intended for local
    development and demonstration, not production deployment.
-   The patient login flow is simulated. It does not provide production
    identity verification, account management, password security, or a
    production OAuth/OIDC authorization service.
