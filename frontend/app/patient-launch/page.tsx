"use client";

import { useEffect } from "react";
import { authorize } from "fhirclient/browser";

export default function PatientLaunchPage() {
  useEffect(() => {
    authorize({
      clientId: "mindstep",

      // SMART Standalone Launch
      iss: "http://localhost:8090/v/r4/sim/WzMsIiIsIiIsIkFVVE8iLDAsMCwwLCJvcGVuaWQgZmhpclVzZXIgbGF1bmNoL3BhdGllbnQgcGF0aWVudC8qLnJlYWQiLCJodHRwOi8vbG9jYWxob3N0OjMwMDAvc21hcnQtY2FsbGJhY2siLCJtaW5kc3RlcCIsIiIsIiIsIiIsIiIsMCwxLCIiXQ/fhir",

      // Ask SMART for the logged-in patient's context
      scope: "openid fhirUser launch/patient patient/*.read",

      redirectUri: "/smart-callback",
    });
  }, []);

  return (
    <main style={{ padding: "40px" }}>
      <h1>Patient Login</h1>
      <p>Connecting to your health record...</p>
    </main>
  );
}