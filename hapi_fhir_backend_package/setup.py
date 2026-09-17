import json
import os
import sys
import time
import requests


FHIR_BASE_URL = "http://localhost:8080/fhir"

FHIR_DATA_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "fhir_data"
)


def wait_for_hapi():
    """
    Wait until the HAPI FHIR server is ready.
    """
    print("\n[1/3] Waiting for HAPI FHIR...")

    for _ in range(60):
        try:
            response = requests.get(
                f"{FHIR_BASE_URL}/metadata",
                timeout=5
            )

            if response.status_code == 200:
                print("HAPI FHIR is ready.")
                return True

        except requests.RequestException:
            pass

        time.sleep(2)

    print("ERROR: HAPI FHIR did not start within the expected time.")
    return False


def get_resource_count(resource_type):
    """
    Return the total number of a FHIR resource type.
    """
    response = requests.get(
        f"{FHIR_BASE_URL}/{resource_type}",
        params={"_summary": "count"},
        timeout=10
    )

    response.raise_for_status()

    return response.json().get("total", 0)


def get_patient_count():
    """
    Return the number of Patient resources currently stored.
    """
    return get_resource_count("Patient")


def import_fhir_data():
    """
    Import all JSON files in fhir_data.

    Transaction and batch Bundles are posted to the FHIR base URL.
    Other resources are posted to /{resourceType}.
    """
    print("\n[2/3] Checking FHIR data...")

    try:
        patient_count = get_patient_count()

    except requests.RequestException as error:
        print(f"ERROR: Cannot check Patient resources: {error}")
        return False

    if patient_count > 0:
        print(
            f"HAPI FHIR already contains "
            f"{patient_count} patients."
        )
        print("Skipping JSON import.")
        return True

    if not os.path.isdir(FHIR_DATA_DIR):
        print(
            f"ERROR: fhir_data directory not found: "
            f"{FHIR_DATA_DIR}"
        )
        return False

    json_files = sorted(
        file_name
        for file_name in os.listdir(FHIR_DATA_DIR)
        if file_name.lower().endswith(".json")
    )

    if not json_files:
        print("ERROR: No JSON files found in fhir_data.")
        return False

    print(
        f"Database is empty. Importing "
        f"{len(json_files)} JSON files..."
    )

    successful = 0
    failed = 0

    for index, file_name in enumerate(json_files, 1):
        file_path = os.path.join(
            FHIR_DATA_DIR,
            file_name
        )

        try:
            with open(
                file_path,
                "r",
                encoding="utf-8"
            ) as file:
                resource = json.load(file)

            resource_type = resource.get("resourceType")

            if not resource_type:
                print(
                    f"FAILED {file_name}: "
                    f"resourceType missing."
                )
                failed += 1
                continue

            # FHIR transaction/batch Bundles must be POSTed
            # to the server base URL, not /Bundle.
            if (
                resource_type == "Bundle"
                and resource.get("type") in ("transaction", "batch")
            ):
                target_url = FHIR_BASE_URL
            else:
                target_url = f"{FHIR_BASE_URL}/{resource_type}"

            response = requests.post(
                target_url,
                json=resource,
                headers={
                    "Content-Type": "application/fhir+json",
                    "Accept": "application/fhir+json"
                },
                timeout=60
            )

            if response.status_code in (200, 201):
                successful += 1
                print(
                    f"[{index}/{len(json_files)}] "
                    f"Imported {file_name}"
                )

            else:
                failed += 1

                print(
                    f"FAILED {file_name}: "
                    f"HTTP {response.status_code}"
                )

                try:
                    outcome = response.json()
                    diagnostics = (
                        outcome.get("issue", [{}])[0]
                        .get("diagnostics")
                    )

                    if diagnostics:
                        print(f"  {diagnostics}")
                    else:
                        print(response.text[:500])

                except ValueError:
                    print(response.text[:500])

        except json.JSONDecodeError as error:
            failed += 1
            print(
                f"FAILED {file_name}: Invalid JSON - {error}"
            )

        except requests.RequestException as error:
            failed += 1
            print(
                f"FAILED {file_name}: Request error - {error}"
            )

        except Exception as error:
            failed += 1
            print(
                f"FAILED {file_name}: {error}"
            )

    print()
    print(
        f"Imported {successful}/{len(json_files)} files."
    )

    if failed:
        print(f"Failed files: {failed}")

    return failed == 0


def verify_data():
    """
    Print the number of each main FHIR resource type.
    """
    print("\n[3/3] Verifying HAPI FHIR data...")

    resource_types = [
        "Patient",
        "Condition",
        "Observation",
        "Encounter",
        "MedicationRequest"
    ]

    try:
        for resource_type in resource_types:
            total = get_resource_count(resource_type)

            print(
                f"{resource_type}: {total}"
            )

    except requests.RequestException as error:
        print(
            f"ERROR during verification: {error}"
        )
        return False

    return True


def main():
    print("=" * 50)
    print("HAPI FHIR Backend Setup")
    print("=" * 50)

    if not wait_for_hapi():
        sys.exit(1)

    if not import_fhir_data():
        print(
            "\nSetup stopped because one or more "
            "FHIR files failed to import."
        )
        sys.exit(1)

    if not verify_data():
        sys.exit(1)

    print("\n" + "=" * 50)
    print("Setup Complete")
    print("=" * 50)

    print("HAPI FHIR:")
    print(FHIR_BASE_URL)

    print()
    print("FHIR Metadata:")
    print(f"{FHIR_BASE_URL}/metadata")

    print()
    print("You can now start the FastAPI backend.")


if __name__ == "__main__":
    main()
