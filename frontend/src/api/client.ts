const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

// Access token lives in memory only (never localStorage) per Security Baseline §3 — reduces XSS token-theft risk.
let accessToken: string | null = null;

export function setAccessToken(token: string | null): void {
  accessToken = token;
}

let refreshInFlight: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = fetch(`${BASE_URL}/auth/refresh`, {
      method: "POST",
      credentials: "include",
    })
      .then(async (res) => {
        if (!res.ok) return false;
        const data = await res.json();
        setAccessToken(data.accessToken);
        return true;
      })
      .catch(() => false)
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

/** Fetch wrapper: attaches the access token, sends the httpOnly refresh cookie, and
 * retries once via silent refresh on a 401 (except for the auth endpoints themselves). */
export async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const isAuthEndpoint = path.startsWith("/auth/");

  const isFormData = options.body instanceof FormData;

  const doFetch = () =>
    fetch(`${BASE_URL}${path}`, {
      ...options,
      credentials: "include",
      headers: {
        // FormData bodies must NOT set Content-Type — the browser adds the multipart boundary itself.
        ...(isFormData ? {} : { "Content-Type": "application/json" }),
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...options.headers,
      },
    });

  let response = await doFetch();
  if (response.status === 401 && !isAuthEndpoint) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      response = await doFetch();
    }
  }
  return response;
}
