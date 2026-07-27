import { useState, type FormEvent } from "react";
import { apiFetch } from "../api/client";

const WAREHOUSE_TYPES = ["", "DRY", "COLD", "OPEN_YARD"] as const;

interface ListingSearchResult {
  id: string;
  title: string;
  city: string;
  address: string;
  warehouseType: string;
  sizeSqm: number;
  weeklyPrice: number;
  status: string;
  photoUrls: string[];
}

export function SearchPage() {
  const [city, setCity] = useState("");
  const [warehouseType, setWarehouseType] = useState<(typeof WAREHOUSE_TYPES)[number]>("");
  const [minSizeSqm, setMinSizeSqm] = useState("");
  const [maxSizeSqm, setMaxSizeSqm] = useState("");
  const [results, setResults] = useState<ListingSearchResult[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    setSearching(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (city) params.set("city", city);
      if (warehouseType) params.set("warehouseType", warehouseType);
      if (minSizeSqm) params.set("minSizeSqm", minSizeSqm);
      if (maxSizeSqm) params.set("maxSizeSqm", maxSizeSqm);

      const response = await apiFetch(`/listings/search?${params.toString()}`);
      if (!response.ok) {
        throw new Error("Search failed, try again");
      }
      setResults(await response.json());
      setHasSearched(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Search failed, try again");
    } finally {
      setSearching(false);
    }
  }

  return (
    <div>
      <h3>Search warehouses</h3>
      <form onSubmit={handleSearch}>
        <label>
          Location
          <input value={city} onChange={(e) => setCity(e.target.value)} />
        </label>
        <label>
          Search type
          <select value={warehouseType} onChange={(e) => setWarehouseType(e.target.value as typeof warehouseType)}>
            {WAREHOUSE_TYPES.map((type) => (
              <option key={type} value={type}>
                {type || "Any"}
              </option>
            ))}
          </select>
        </label>
        <label>
          Min size (sqm)
          <input type="number" min="0" step="0.01" value={minSizeSqm} onChange={(e) => setMinSizeSqm(e.target.value)} />
        </label>
        <label>
          Max size (sqm)
          <input type="number" min="0" step="0.01" value={maxSizeSqm} onChange={(e) => setMaxSizeSqm(e.target.value)} />
        </label>
        <button type="submit" disabled={searching}>
          {searching ? "Searching..." : "Search"}
        </button>
      </form>
      {error && <p role="alert">{error}</p>}
      {hasSearched && results.length === 0 && !error && <p>No warehouses match — try widening your filters.</p>}
      <ul>
        {results.map((listing) => (
          <li key={listing.id}>
            <strong>{listing.title}</strong> — {listing.city}, {listing.warehouseType}, {listing.sizeSqm} sqm,{" "}
            {listing.weeklyPrice}/week
          </li>
        ))}
      </ul>
    </div>
  );
}
