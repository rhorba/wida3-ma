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

interface BookingOutcome {
  status: "success" | "error";
  message: string;
}

export function SearchPage({ onBooked }: { onBooked?: () => void } = {}) {
  const [city, setCity] = useState("");
  const [warehouseType, setWarehouseType] = useState<(typeof WAREHOUSE_TYPES)[number]>("");
  const [minSizeSqm, setMinSizeSqm] = useState("");
  const [maxSizeSqm, setMaxSizeSqm] = useState("");
  const [availableFrom, setAvailableFrom] = useState("");
  const [availableUntil, setAvailableUntil] = useState("");
  const [results, setResults] = useState<ListingSearchResult[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [searching, setSearching] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [bookingListingId, setBookingListingId] = useState<string | null>(null);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [booking, setBooking] = useState(false);
  const [bookingOutcome, setBookingOutcome] = useState<Record<string, BookingOutcome>>({});

  function openBookingForm(listingId: string) {
    setBookingListingId(listingId);
    setStartDate("");
    setEndDate("");
  }

  async function handleBook(listingId: string) {
    setBooking(true);
    setBookingOutcome((prev) => ({ ...prev, [listingId]: { status: "success", message: "" } }));
    try {
      const response = await apiFetch("/bookings", {
        method: "POST",
        body: JSON.stringify({ listingId, startDate, endDate }),
      });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) {
        throw new Error(body.message ?? "Booking failed, try again");
      }
      const message =
        body.status === "CONFIRMED"
          ? `Booked! Access code: ${body.accessCode}`
          : `Booking failed: ${body.paymentFailureReason ?? "payment declined"}`;
      setBookingOutcome((prev) => ({
        ...prev,
        [listingId]: { status: body.status === "CONFIRMED" ? "success" : "error", message },
      }));
      if (body.status === "CONFIRMED") {
        setBookingListingId(null);
        onBooked?.();
      }
    } catch (err) {
      setBookingOutcome((prev) => ({
        ...prev,
        [listingId]: { status: "error", message: err instanceof Error ? err.message : "Booking failed, try again" },
      }));
    } finally {
      setBooking(false);
    }
  }

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
      if (availableFrom) params.set("availableFrom", availableFrom);
      if (availableUntil) params.set("availableUntil", availableUntil);

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
        <label>
          Available from
          <input type="date" value={availableFrom} onChange={(e) => setAvailableFrom(e.target.value)} />
        </label>
        <label>
          Available until
          <input type="date" value={availableUntil} onChange={(e) => setAvailableUntil(e.target.value)} />
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
            {bookingListingId === listing.id ? (
              <div>
                <label>
                  Start date
                  <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                </label>
                <label>
                  End date
                  <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                </label>
                <button type="button" disabled={booking || !startDate || !endDate} onClick={() => handleBook(listing.id)}>
                  {booking ? "Booking..." : "Confirm booking"}
                </button>
                <button type="button" onClick={() => setBookingListingId(null)}>
                  Cancel
                </button>
              </div>
            ) : (
              <button type="button" onClick={() => openBookingForm(listing.id)}>
                Book
              </button>
            )}
            {bookingOutcome[listing.id]?.message && (
              <p role={bookingOutcome[listing.id].status === "error" ? "alert" : "status"}>
                {bookingOutcome[listing.id].message}
              </p>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
