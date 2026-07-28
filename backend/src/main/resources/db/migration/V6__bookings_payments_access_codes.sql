CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id        UUID NOT NULL REFERENCES listings(id),
  renter_id         UUID NOT NULL REFERENCES users(id),
  start_date        DATE NOT NULL,
  end_date          DATE NOT NULL,
  total_price       NUMERIC(10,2) NOT NULL,
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
  idempotency_key   VARCHAR(100),
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_booking_dates CHECK (end_date > start_date)
);

CREATE UNIQUE INDEX idx_bookings_idempotency_key ON bookings(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX idx_bookings_listing ON bookings(listing_id);
CREATE INDEX idx_bookings_renter ON bookings(renter_id);

-- Defense-in-depth alongside the app-level row lock: no two CONFIRMED bookings may
-- overlap date ranges on the same listing, enforced by Postgres itself.
ALTER TABLE bookings ADD CONSTRAINT excl_bookings_no_overlap_confirmed
  EXCLUDE USING gist (
    listing_id WITH =,
    daterange(start_date, end_date, '[)') WITH &&
  ) WHERE (status = 'CONFIRMED');

CREATE TABLE payments (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id        UUID NOT NULL UNIQUE REFERENCES bookings(id),
  provider          VARCHAR(50) NOT NULL DEFAULT 'MOCK',
  provider_ref      VARCHAR(255),
  amount            NUMERIC(10,2) NOT NULL,
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_booking ON payments(booking_id);

CREATE TABLE access_codes (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id  UUID NOT NULL UNIQUE REFERENCES bookings(id),
  code        VARCHAR(20) NOT NULL,
  issued_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at  TIMESTAMPTZ NOT NULL
);
