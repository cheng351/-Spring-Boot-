"use client";

import { useEffect } from "react";
import { authorize } from "fhirclient/browser";

export default function SmartLaunchPage() {
  useEffect(() => {
    authorize({
      clientId: "mindstep",
      scope: "launch openid fhirUser patient/*.read",
      redirectUri: "/smart-callback",
    });
  }, []);

  return (
    <main style={{ padding: "40px" }}>
      <h1>Launching MindStep...</h1>
      <p>Connecting to SMART on FHIR.</p>
    </main>
  );
}