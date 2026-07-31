import { useState, type FormEvent } from "react";
import { apiFetch } from "../api/client";

const WAREHOUSE_TYPES = ["DRY", "COLD", "OPEN_YARD"] as const;
const MAX_PHOTOS = 10;

interface PhotoPreview {
  file: File;
  previewUrl: string;
}

export function CreateListingForm({ onCreated }: { onCreated?: () => void } = {}) {
  const [title, setTitle] = useState("");
  const [city, setCity] = useState("");
  const [address, setAddress] = useState("");
  const [warehouseType, setWarehouseType] = useState<(typeof WAREHOUSE_TYPES)[number]>("DRY");
  const [sizeSqm, setSizeSqm] = useState("");
  const [weeklyPrice, setWeeklyPrice] = useState("");
  const [photos, setPhotos] = useState<PhotoPreview[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function handlePhotoSelection(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    const combined = [...photos, ...files.map((file) => ({ file, previewUrl: URL.createObjectURL(file) }))];
    setPhotos(combined.slice(0, MAX_PHOTOS));
  }

  function removePhoto(index: number) {
    setPhotos((prev) => prev.filter((_, i) => i !== index));
  }

  async function uploadPhotos(): Promise<string[]> {
    const urls: string[] = [];
    for (const photo of photos) {
      const formData = new FormData();
      formData.append("file", photo.file);
      const response = await apiFetch("/files/upload", { method: "POST", body: formData });
      if (!response.ok) {
        const body = await response.json().catch(() => ({ message: "Upload failed" }));
        throw new Error(body.message ?? "Upload failed");
      }
      const data = await response.json();
      urls.push(data.url);
    }
    return urls;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccess(null);
    try {
      const photoUrls = await uploadPhotos();
      const response = await apiFetch("/listings", {
        method: "POST",
        body: JSON.stringify({
          title,
          city,
          address,
          warehouseType,
          sizeSqm: Number(sizeSqm),
          weeklyPrice: Number(weeklyPrice),
          photoUrls,
        }),
      });
      if (!response.ok) {
        const body = await response.json().catch(() => ({ message: "Failed to create listing" }));
        throw new Error(body.message ?? "Failed to create listing");
      }
      setSuccess("Listing created — pending admin approval.");
      setTitle("");
      setCity("");
      setAddress("");
      setSizeSqm("");
      setWeeklyPrice("");
      setPhotos([]);
      onCreated?.();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit}>
      <h3>Create a listing</h3>
      <label>
        Title
        <input value={title} onChange={(e) => setTitle(e.target.value)} required />
      </label>
      <label>
        City
        <input value={city} onChange={(e) => setCity(e.target.value)} required />
      </label>
      <label>
        Address
        <input value={address} onChange={(e) => setAddress(e.target.value)} required />
      </label>
      <label>
        Warehouse type
        <select value={warehouseType} onChange={(e) => setWarehouseType(e.target.value as typeof warehouseType)}>
          {WAREHOUSE_TYPES.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
      </label>
      <label>
        Size (sqm)
        <input
          type="number"
          min="0"
          step="0.01"
          value={sizeSqm}
          onChange={(e) => setSizeSqm(e.target.value)}
          required
        />
      </label>
      <label>
        Weekly price
        <input
          type="number"
          min="0"
          step="0.01"
          value={weeklyPrice}
          onChange={(e) => setWeeklyPrice(e.target.value)}
          required
        />
      </label>
      <label>
        Photos (up to {MAX_PHOTOS})
        <input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={handlePhotoSelection} />
      </label>
      <div>
        {photos.map((photo, index) => (
          <span key={photo.previewUrl}>
            <img src={photo.previewUrl} alt="" width={80} height={80} />
            <button type="button" onClick={() => removePhoto(index)}>
              Remove
            </button>
          </span>
        ))}
      </div>
      {error && <p role="alert">{error}</p>}
      {success && <p>{success}</p>}
      <button type="submit" disabled={submitting}>
        {submitting ? "Creating..." : "Create listing"}
      </button>
    </form>
  );
}
