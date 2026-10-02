import { useMemo, useState } from "react";
import useFetchPatients from "@/hooks/useFetchPatients";

export default function PatientLogin() {
  const { patients, isInitialLoading } = useFetchPatients();

  const [selectedPatientId, setSelectedPatientId] = useState("");
  const [password, setPassword] = useState("");

  const searchParams = useMemo(
    () => new URLSearchParams(window.location.search),
    []
  );

  const getPatientName = (patient: (typeof patients)[number]) => {
    const name = patient.name?.[0];

    if (!name) {
      return patient.id ?? "Unknown patient";
    }

    const given = name.given?.join(" ") ?? "";
    const family = name.family ?? "";

    return `${given} ${family}`.trim() || patient.id || "Unknown patient";
  };

  const handleLogin = () => {
    if (!selectedPatientId) {
      alert("Please select a patient.");
      return;
    }

    if (!password) {
      alert("Please enter a password.");
      return;
    }

    /*
     * Preserve all OAuth parameters that the SMART authorization
     * server originally sent to /patient-login.
     */
    const params = new URLSearchParams(searchParams);

    params.set("patient", selectedPatientId);
    params.set("login_success", "1");
    params.set("auth_success", "1");

    /*
     * Return to the SMART authorization endpoint.
     *
     * The current SMART simulation identifier is preserved in the
     * authorization URL supplied in the "aud" parameter.
     */
    const aud = params.get("aud");

    if (!aud) {
      alert("SMART authorization information is missing.");
      return;
    }

    const authorizeUrl = aud
      .replace(/\/fhir\/?$/, "/auth/authorize");

    window.location.href =
      `${authorizeUrl}?${params.toString()}`;
  };

  return (
    <main
      style={{
        maxWidth: "520px",
        margin: "80px auto",
        padding: "32px",
        fontFamily: "Arial, sans-serif",
      }}
    >
      <h1>Patient Login</h1>

      <p>
        Sign in to access your MindStep health information.
      </p>

      <div style={{ marginTop: "24px" }}>
        <label htmlFor="patient">
          <strong>Patient</strong>
        </label>

        <select
          id="patient"
          value={selectedPatientId}
          onChange={(event) =>
            setSelectedPatientId(event.target.value)
          }
          disabled={isInitialLoading}
          style={{
            display: "block",
            width: "100%",
            marginTop: "8px",
            padding: "10px",
          }}
        >
          <option value="">
            {isInitialLoading
              ? "Loading patients..."
              : "Select a patient"}
          </option>

          {patients.map((patient) => (
            <option
              key={patient.id}
              value={patient.id}
            >
              {getPatientName(patient)}
            </option>
          ))}
        </select>
      </div>

      <div style={{ marginTop: "20px" }}>
        <label htmlFor="password">
          <strong>Password</strong>
        </label>

        <input
          id="password"
          type="password"
          value={password}
          onChange={(event) =>
            setPassword(event.target.value)
          }
          placeholder="Enter password"
          style={{
            display: "block",
            width: "100%",
            boxSizing: "border-box",
            marginTop: "8px",
            padding: "10px",
          }}
        />
      </div>

      <p
        style={{
          marginTop: "12px",
          fontSize: "14px",
          color: "#666",
        }}
      >
        Demo login: any non-empty password will be accepted.
      </p>

      <button
        type="button"
        onClick={handleLogin}
        style={{
          width: "100%",
          marginTop: "20px",
          padding: "12px",
          cursor: "pointer",
        }}
      >
        Login
      </button>
    </main>
  );
}