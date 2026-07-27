import { useEffect, useState } from "react";
import { apiFetch } from "../api/client";

interface PendingListing {
  id: string;
  title: string;
  city: string;
  warehouseType: string;
  sizeSqm: number;
  weeklyPrice: number;
}

export function AdminApprovalQueue() {
  const [pending, setPending] = useState<PendingListing[]>([]);
  const [rejectReasons, setRejectReasons] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  async function loadPending() {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFetch("/listings/pending");
      if (!response.ok) throw new Error("Failed to load pending listings");
      setPending(await response.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load pending listings");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadPending();
  }, []);

  async function approve(id: string) {
    setError(null);
    try {
      const response = await apiFetch(`/listings/${id}/approve`, { method: "PATCH" });
      if (!response.ok) throw new Error("Failed to approve listing");
      await loadPending();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to approve listing");
    }
  }

  async function reject(id: string) {
    const reason = rejectReasons[id]?.trim();
    if (!reason) {
      setError("A reject reason is required");
      return;
    }
    setError(null);
    try {
      const response = await apiFetch(`/listings/${id}/reject`, {
        method: "PATCH",
        body: JSON.stringify({ reason }),
      });
      if (!response.ok) throw new Error("Failed to reject listing");
      await loadPending();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to reject listing");
    }
  }

  if (loading) return <p>Loading pending listings...</p>;

  return (
    <div>
      <h3>Pending listing approvals</h3>
      {error && <p role="alert">{error}</p>}
      {pending.length === 0 && <p>Nothing pending review.</p>}
      <ul>
        {pending.map((listing) => (
          <li key={listing.id}>
            <strong>{listing.title}</strong> — {listing.city}, {listing.warehouseType}, {listing.sizeSqm} sqm,{" "}
            {listing.weeklyPrice}/week
            <div>
              <button type="button" onClick={() => approve(listing.id)}>
                Approve
              </button>
              <textarea
                placeholder="Reason for rejection"
                value={rejectReasons[listing.id] ?? ""}
                onChange={(e) => setRejectReasons((prev) => ({ ...prev, [listing.id]: e.target.value }))}
              />
              <button type="button" onClick={() => reject(listing.id)}>
                Reject
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
