"use client";

import { useEffect, useState } from "react";
import { ready } from "fhirclient/browser";

export default function SmartCallbackPage() {
  const [status, setStatus] = useState(
    "Connecting to SMART on FHIR..."
  );

  useEffect(() => {
    ready()
      .then((client) => {
        const patientId = client.patient?.id;

        if (!patientId) {
          setStatus(
            "SMART connection successful, but no patient was selected."
          );
          return;
        }

        sessionStorage.setItem("smartPatientId", patientId);

        setStatus(
          `SMART connection successful. Patient ID: ${patientId}`
        );

        window.location.href = "/";
      })
      .catch((error) => {
        console.error("SMART callback error:", error);
        setStatus("SMART connection failed.");
      });
  }, []);

  return (
    <main style={{ padding: "40px" }}>
      <h1>SMART on FHIR</h1>
      <p>{status}</p>
    </main>
  );
}