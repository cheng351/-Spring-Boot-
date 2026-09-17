from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from fhir_reader import (
    get_all_patients,
    get_patient_name,
    get_patient_data
)


app = FastAPI(
    title="Parkinson's FHIR API",
    version="1.0.0"
)


app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def root():
    return {
        "message": "Parkinson's FHIR API is running"
    }


@app.get("/api/patients")
def list_patients():
    """
    Return all patients stored in HAPI FHIR.
    """

    try:
        patients = get_all_patients()

        result = []

        for patient in patients:
            result.append({
                "id": patient.get("id"),
                "name": get_patient_name(patient),
                "gender": patient.get("gender"),
                "birthDate": patient.get("birthDate")
            })

        return {
            "count": len(result),
            "patients": result
        }

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(e)
        )


@app.get("/api/patient/{patient_id}")
def get_patient(patient_id: str):
    """
    Return complete FHIR data
    for one patient.
    """

    try:
        data = get_patient_data(
            patient_id
        )

        return data

    except Exception as e:
        raise HTTPException(
            status_code=500,
            detail=str(e)
        )
