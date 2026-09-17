import requests


FHIR_BASE_URL = "http://localhost:8080/fhir"


def get_resource(resource_type, resource_id):
    """
    Read a single FHIR resource by ID.
    Example:
    Patient/{id}
    """
    url = f"{FHIR_BASE_URL}/{resource_type}/{resource_id}"

    response = requests.get(url)
    response.raise_for_status()

    return response.json()


def search_resources(resource_type, patient_id):
    """
    Search all resources related to one patient.
    Example:
    Observation?patient={patient_id}
    """
    url = f"{FHIR_BASE_URL}/{resource_type}"

    params = {
        "patient": patient_id,
        "_count": 100
    }

    response = requests.get(url, params=params)
    response.raise_for_status()

    bundle = response.json()

    resources = []

    for entry in bundle.get("entry", []):
        resource = entry.get("resource")

        if resource:
            resources.append(resource)

    return resources


def get_all_patients():
    """
    Retrieve all patients from HAPI FHIR.
    """

    url = f"{FHIR_BASE_URL}/Patient"

    params = {
        "_count": 100
    }

    response = requests.get(
        url,
        params=params
    )

    response.raise_for_status()

    bundle = response.json()

    patients = []

    for entry in bundle.get("entry", []):
        resource = entry.get("resource")

        if resource:
            patients.append(resource)

    return patients


def get_patient_name(patient):
    """
    Extract a readable patient name.
    """

    names = patient.get("name", [])

    if not names:
        return "Unknown Patient"

    name = names[0]

    given = " ".join(
        name.get("given", [])
    )

    family = name.get(
        "family",
        ""
    )

    full_name = f"{given} {family}".strip()

    return full_name


def select_patient():
    """
    Display all patients and allow the user
    to select one by number.
    """

    patients = get_all_patients()

    print("\n====================================")
    print("Available Patients")
    print("====================================")

    for i, patient in enumerate(
        patients,
        1
    ):

        patient_name = get_patient_name(
            patient
        )

        patient_id = patient.get(
            "id",
            "Unknown"
        )

        print(
            f"{i}. {patient_name}"
        )

        print(
            f"   ID: {patient_id}"
        )

    while True:

        try:

            choice = int(
                input(
                    "\nSelect patient number: "
                )
            )

            if 1 <= choice <= len(patients):

                selected_patient = patients[
                    choice - 1
                ]

                return selected_patient.get(
                    "id"
                )

            else:

                print(
                    "Please enter a valid patient number."
                )

        except ValueError:

            print(
                "Please enter a number."
            )


def get_patient_data(patient_id):
    """
    Retrieve all main FHIR resources
    for one patient.
    """

    patient = get_resource(
        "Patient",
        patient_id
    )

    conditions = search_resources(
        "Condition",
        patient_id
    )

    observations = search_resources(
        "Observation",
        patient_id
    )

    encounters = search_resources(
        "Encounter",
        patient_id
    )

    medication_requests = search_resources(
        "MedicationRequest",
        patient_id
    )

    return {
        "patient": patient,
        "conditions": conditions,
        "observations": observations,
        "encounters": encounters,
        "medication_requests": medication_requests
    }


def print_patient_summary(data):

    patient = data["patient"]

    print("\n====================================")
    print("Patient Summary")
    print("====================================")

    print(
        "Patient ID:",
        patient.get("id")
    )

    print(
        "Name:",
        get_patient_name(patient)
    )

    print(
        "Gender:",
        patient.get("gender")
    )

    print(
        "Birth Date:",
        patient.get("birthDate")
    )

    print("\nFHIR Resources")

    print(
        "Conditions:",
        len(data["conditions"])
    )

    print(
        "Observations:",
        len(data["observations"])
    )

    print(
        "Encounters:",
        len(data["encounters"])
    )

    print(
        "Medication Requests:",
        len(
            data["medication_requests"]
        )
    )


def print_clinical_details(data):

    print("\n====================================")
    print("Conditions")
    print("====================================")

    for i, condition in enumerate(
        data["conditions"],
        1
    ):

        code = condition.get(
            "code",
            {}
        )

        condition_name = code.get(
            "text",
            "Unknown"
        )

        if condition_name == "Unknown":

            coding = code.get(
                "coding",
                []
            )

            if coding:

                condition_name = coding[0].get(
                    "display",
                    "Unknown"
                )

        print(
            f"{i}. {condition_name}"
        )


    print("\n====================================")
    print("Observations")
    print("====================================")

    for i, observation in enumerate(
        data["observations"],
        1
    ):

        code = observation.get(
            "code",
            {}
        )

        observation_name = code.get(
            "text",
            "Unknown"
        )

        date = observation.get(
            "effectiveDateTime",
            "Unknown"
        )

        value = "Unknown"

        if "valueQuantity" in observation:

            quantity = observation[
                "valueQuantity"
            ]

            value = (
                f'{quantity.get("value", "")} '
                f'{quantity.get("unit", "")}'
            )

        elif "valueString" in observation:

            value = observation[
                "valueString"
            ]

        elif "valueCodeableConcept" in observation:

            concept = observation[
                "valueCodeableConcept"
            ]

            value = concept.get(
                "text",
                "Unknown"
            )

        print(
            f"{i}. {observation_name}"
        )

        print(
            f"   Date: {date}"
        )

        print(
            f"   Value: {value}"
        )


    print("\n====================================")
    print("Encounters")
    print("====================================")

    for i, encounter in enumerate(
        data["encounters"],
        1
    ):

        print(
            f"{i}. ID: {encounter.get('id')}"
        )

        print(
            f"   Status: {encounter.get('status')}"
        )

        period = encounter.get(
            "period",
            {}
        )

        print(
            f"   Start: {period.get('start', 'Unknown')}"
        )

        print(
            f"   End: {period.get('end', 'Unknown')}"
        )


    print("\n====================================")
    print("Medication Requests")
    print("====================================")

    for i, medication in enumerate(
        data["medication_requests"],
        1
    ):

        medication_name = "Unknown"

        concept = medication.get(
            "medicationCodeableConcept",
            {}
        )

        medication_name = concept.get(
            "text",
            "Unknown"
        )

        if medication_name == "Unknown":

            coding = concept.get(
                "coding",
                []
            )

            if coding:

                medication_name = coding[0].get(
                    "display",
                    "Unknown"
                )

        print(
            f"{i}. {medication_name}"
        )

        print(
            f"   Status: {medication.get('status')}"
        )


if __name__ == "__main__":

    patient_id = select_patient()

    patient_data = get_patient_data(
        patient_id
    )

    print_patient_summary(
        patient_data
    )

    print_clinical_details(
        patient_data
    )