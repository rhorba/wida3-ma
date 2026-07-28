import { useEffect, useState } from "react";
import { apiFetch } from "../api/client";

interface Booking {
  id: string;
  listingId: string;
  listingTitle: string;
  startDate: string;
  endDate: string;
  totalPrice: number;
  status: string;
  accessCode: string | null;
  paymentFailureReason: string | null;
}

export function MyBookings() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancellingId, setCancellingId] = useState<string | null>(null);

  async function loadBookings() {
    setLoading(true);
    setError(null);
    try {
      const response = await apiFetch("/bookings");
      if (!response.ok) throw new Error("Failed to load bookings");
      setBookings(await response.json());
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load bookings");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadBookings();
  }, []);

  async function cancelBooking(id: string) {
    setCancellingId(id);
    setError(null);
    try {
      const response = await apiFetch(`/bookings/${id}/cancel`, { method: "POST" });
      const body = await response.json().catch(() => ({}));
      if (!response.ok) throw new Error(body.message ?? "Failed to cancel booking");
      await loadBookings();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to cancel booking");
    } finally {
      setCancellingId(null);
    }
  }

  if (loading) return <p>Loading bookings...</p>;

  return (
    <div>
      <h3>My bookings</h3>
      {error && <p role="alert">{error}</p>}
      {bookings.length === 0 && <p>No bookings yet.</p>}
      <ul>
        {bookings.map((booking) => (
          <li key={booking.id}>
            <strong>{booking.listingTitle}</strong> — {booking.startDate} to {booking.endDate}, {booking.totalPrice},{" "}
            {booking.status}
            {booking.accessCode && <span> — access code: {booking.accessCode}</span>}
            {booking.paymentFailureReason && <span> — {booking.paymentFailureReason}</span>}
            {booking.status === "CONFIRMED" && (
              <button type="button" disabled={cancellingId === booking.id} onClick={() => cancelBooking(booking.id)}>
                {cancellingId === booking.id ? "Cancelling..." : "Cancel"}
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
