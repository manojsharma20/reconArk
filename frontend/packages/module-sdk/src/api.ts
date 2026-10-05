/** RFC 9457 problem details as returned by every reconArk API. */
export interface Problem {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: Problem,
  ) {
    super(problem.detail ?? problem.title ?? `HTTP ${status}`);
  }
}

function csrfToken(): string | undefined {
  const match = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='));
  return match ? decodeURIComponent(match.split('=')[1] ?? '') : undefined;
}

/**
 * Calls the BFF. The browser holds only the session cookie; the BFF attaches tokens (ADR-0031). Mutations carry the
 * CSRF token and an Idempotency-Key.
 */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? 'GET').toUpperCase();
  const headers = new Headers(init.headers);
  headers.set('Accept', 'application/json');
  if (init.body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (method !== 'GET' && method !== 'HEAD') {
    const token = csrfToken();
    if (token) headers.set('X-XSRF-TOKEN', token);
    if (!headers.has('Idempotency-Key')) headers.set('Idempotency-Key', crypto.randomUUID());
  }
  const res = await fetch(path, { ...init, method, headers, credentials: 'same-origin' });
  const text = await res.text();
  const body: unknown = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    throw new ApiError(res.status, (body as Problem | undefined) ?? { status: res.status });
  }
  return body as T;
}

export const postJson = <T>(path: string, payload: unknown): Promise<T> =>
  api<T>(path, { method: 'POST', body: JSON.stringify(payload) });
