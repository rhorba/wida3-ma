import { useEffect, useState, type FormEvent } from "react";
import { apiFetch } from "../api/client";

const WAREHOUSE_TYPES = ["DRY", "COLD", "OPEN_YARD"] as const;

interface Listing {
  id: string;
  title: string;
  city: string;
  address: string;
  warehouseType: string;
  sizeSqm: number;
  weeklyPrice: number;
  status: string;
  rejectionReason: string | null;
}

interface EditFields {
  title: string;
  city: string;
  address: string;
  warehouseType: (typeof WAREHOUSE_TYPES)[number];
  sizeSqm: string;
  weeklyPrice: string;
}

export function MyListings() {
  const [listings, setListings] = useState<Listing[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editFields, setEditFields] = useState<EditFields | null>(null);
  const [saving, setSaving] = useState(false);
  const [deactivatingId, setDeactivatingId] = useState<string | null>(null);

  async function loadListings() {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFetch("/listings/mine");
      if (!response.ok) throw new Error("Failed to load your listings");
      setListings(await response.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load your listings");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadListings();
  }, []);

  function startEdit(listing: Listing) {
    setEditingId(listing.id);
    setEditFields({
      title: listing.title,
      city: listing.city,
      address: listing.address,
      warehouseType: listing.warehouseType as EditFields["warehouseType"],
      sizeSqm: String(listing.sizeSqm),
      weeklyPrice: String(listing.weeklyPrice),
    });
  }

  async function handleSave(e: FormEvent, id: string) {
    e.preventDefault();
    if (!editFields) return;
    setSaving(true);
    setError(null);
    try {
      const response = await apiFetch(`/listings/${id}`, {
        method: "PUT",
        body: JSON.stringify({
          ...editFields,
          sizeSqm: Number(editFields.sizeSqm),
          weeklyPrice: Number(editFields.weeklyPrice),
        }),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(body.message ?? "Failed to update listing");
      setEditingId(null);
      setEditFields(null);
      await loadListings();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update listing");
    } finally {
      setSaving(false);
    }
  }

  async function handleDeactivate(id: string) {
    setDeactivatingId(id);
    setError(null);
    try {
      const response = await apiFetch(`/listings/${id}/deactivate`, { method: "PATCH" });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(body.message ?? "Failed to deactivate listing");
      await loadListings();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to deactivate listing");
    } finally {
      setDeactivatingId(null);
    }
  }

  if (loading) return <p>Loading your listings...</p>;

  return (
    <div>
      <h3>My listings</h3>
      {error && <p role="alert">{error}</p>}
      {listings.length === 0 && <p>You haven't created any listings yet.</p>}
      <ul>
        {listings.map((listing) => (
          <li key={listing.id}>
            {editingId === listing.id && editFields ? (
              <form onSubmit={(e) => handleSave(e, listing.id)}>
                <label>
                  Edit title
                  <input
                    value={editFields.title}
                    onChange={(e) => setEditFields({ ...editFields, title: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Edit city
                  <input
                    value={editFields.city}
                    onChange={(e) => setEditFields({ ...editFields, city: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Edit address
                  <input
                    value={editFields.address}
                    onChange={(e) => setEditFields({ ...editFields, address: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Edit warehouse type
                  <select
                    value={editFields.warehouseType}
                    onChange={(e) =>
                      setEditFields({ ...editFields, warehouseType: e.target.value as EditFields["warehouseType"] })
                    }
                  >
                    {WAREHOUSE_TYPES.map((type) => (
                      <option key={type} value={type}>
                        {type}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Edit size (sqm)
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={editFields.sizeSqm}
                    onChange={(e) => setEditFields({ ...editFields, sizeSqm: e.target.value })}
                    required
                  />
                </label>
                <label>
                  Edit weekly price
                  <input
                    type="number"
                    min="0"
                    step="0.01"
                    value={editFields.weeklyPrice}
                    onChange={(e) => setEditFields({ ...editFields, weeklyPrice: e.target.value })}
                    required
                  />
                </label>
                <button type="submit" disabled={saving}>
                  {saving ? "Saving..." : "Save"}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEditingId(null);
                    setEditFields(null);
                  }}
                >
                  Cancel
                </button>
              </form>
            ) : (
              <>
                <strong>{listing.title}</strong> — {listing.city}, {listing.warehouseType}, {listing.sizeSqm} sqm,{" "}
                {listing.weeklyPrice}/week — {listing.status}
                {listing.rejectionReason && <span> ({listing.rejectionReason})</span>}
                {listing.status !== "INACTIVE" && (
                  <>
                    <button type="button" onClick={() => startEdit(listing)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      disabled={deactivatingId === listing.id}
                      onClick={() => handleDeactivate(listing.id)}
                    >
                      {deactivatingId === listing.id ? "Deactivating..." : "Deactivate"}
                    </button>
                  </>
                )}
              </>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
