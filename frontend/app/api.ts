/**
 * Calls into the Spring backend.
 *
 * Two endpoints, kept apart on purpose. The profile carries the patient's name and is rendered
 * by this UI only; the chat endpoint works from a de-identified snapshot on the server side, so
 * nothing identifying is sent to the language model. Do not start posting profile fields into
 * the chat request to "give it more context" — that quietly undoes the separation.
 */

const API_BASE =
  process.env.NEXT_PUBLIC_API_BASE ?? 'http://localhost:8081';

/**
 * Which patient this prototype opens. A SMART-on-FHIR launch supplies this from the
 * authorisation context; until that is wired up it comes from the environment, defaulting to
 * the first patient in the team dataset.
 */
export const PATIENT_ID =
  process.env.NEXT_PUBLIC_PATIENT_ID ?? '25b3eb56-3d12-4496-a4ba-f5fd0913b3c9';

export type PatientProfile = {
  patientId: string;
  name: string;
  age: number;
  gender: string;
  conditions: string[];
};

export type GuidanceResponse = {
  patientId: string;
  plainLanguageGuidance: string;
  generatedAt: string;
};

/** The shape the backend returns for a handled failure. */
type ApiError = { error?: string; message?: string };

export class BackendError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'BackendError';
    this.status = status;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...init?.headers },
    });
  } catch {
    // Network-level failure: the backend is not running, or CORS rejected the request.
    throw new BackendError(0, 'We could not reach the service. Please try again shortly.');
  }

  if (!response.ok) {
    const body = (await response.json().catch(() => ({}))) as ApiError;
    throw new BackendError(
      response.status,
      body.message ?? 'Something went wrong at our end. Please try again later.',
    );
  }

  return response.json() as Promise<T>;
}

export function fetchProfile(patientId: string) {
  return request<PatientProfile>(`/api/support/profile/${encodeURIComponent(patientId)}`);
}

export function fetchOpeningMessage(patientId: string) {
  return request<GuidanceResponse>(`/api/support/guidance/${encodeURIComponent(patientId)}`);
}

export function askQuestion(patientId: string, question: string) {
  return request<GuidanceResponse>('/api/support/ask', {
    method: 'POST',
    body: JSON.stringify({ patientId, question }),
  });
}
