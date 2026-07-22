import { useState } from "react";
import { apiFetch } from "../api/client";
import { useAuth } from "../auth/AuthContext";

export function DashboardPage() {
  const { user, logout } = useAuth();
  const [refreshResult, setRefreshResult] = useState<string | null>(null);

  async function handleForceRefresh() {
    // Manually exercises the silent-refresh path for verification (real 401-triggered
    // refresh happens automatically inside apiFetch on any protected call).
    const response = await apiFetch("/auth/refresh", { method: "POST" });
    setRefreshResult(response.ok ? "Refreshed OK" : `Refresh failed (${response.status})`);
  }

  return (
    <div>
      <h2>Logged in as {user?.email}</h2>
      <p>Roles: {user?.roles.join(", ")}</p>
      <button onClick={handleForceRefresh}>Force refresh</button>
      {refreshResult && <p>{refreshResult}</p>}
      <button onClick={() => logout()}>Log out</button>
    </div>
  );
}
