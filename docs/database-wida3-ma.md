# Database Design: Wida3.ma
**Architecture Reference**: docs/architecture-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: DBA

## 1. Database Selection
- **Engine**: PostgreSQL 16 + PostGIS extension
- **Rationale**: Relational data with clear entities/relationships (users, listings, bookings); PostGIS needed for city/geo-based listing search (FR-2). YAGNI default confirmed — no separate search engine needed at MVP volume.
- **Hosting**: Self-hosted in the same docker-compose stack as the API (single VPS, MVP). Revisit managed Postgres only if backup/HA requirements outgrow manual ops.

## 2. Entity-Relationship Model
```
User ──1:N──> Listing              (via listings.owner_id)
User ──N:N──> Role                 (via user_roles)
Listing ──1:N──> ListingPhoto
Listing ──1:N──> Booking
User ──1:N──> Booking              (via bookings.renter_id)
Booking ──1:1──> Payment
Booking ──1:1──> AccessCode
```

## 3. Schema Design
```sql
-- Table: users
CREATE TABLE users (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name     VARCHAR(255) NOT NULL,
  phone         VARCHAR(30),
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: roles (seed: OWNER, RENTER, ADMIN)
CREATE TABLE roles (
  id    SMALLSERIAL PRIMARY KEY,
  name  VARCHAR(20) NOT NULL UNIQUE
);

-- Table: user_roles
CREATE TABLE user_roles (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id SMALLINT NOT NULL REFERENCES roles(id),
  PRIMARY KEY (user_id, role_id)
);

-- Table: listings
CREATE TABLE listings (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id      UUID NOT NULL REFERENCES users(id),
  title         VARCHAR(255) NOT NULL,
  city          VARCHAR(100) NOT NULL,
  address       VARCHAR(500) NOT NULL,
  location      GEOGRAPHY(POINT, 4326),      -- PostGIS: lat/lng, nullable until geocoded
  warehouse_type VARCHAR(50) NOT NULL,        -- e.g. DRY, COLD, OPEN_YARD
  size_sqm      NUMERIC(10,2) NOT NULL,
  weekly_price  NUMERIC(10,2) NOT NULL,
  status        VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL', -- PENDING_APPROVAL, ACTIVE, INACTIVE, REJECTED
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: listing_photos
CREATE TABLE listing_photos (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id  UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  file_url    VARCHAR(1000) NOT NULL,
  sort_order  SMALLINT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: bookings
CREATE TABLE bookings (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id    UUID NOT NULL REFERENCES listings(id),
  renter_id     UUID NOT NULL REFERENCES users(id),
  start_date    DATE NOT NULL,
  end_date      DATE NOT NULL,                -- always a whole number of weeks from start_date
  total_price   NUMERIC(10,2) NOT NULL,        -- server-calculated, never trust client
  status        VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT', -- PENDING_PAYMENT, CONFIRMED, CANCELLED, COMPLETED
  created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_week_multiple CHECK (end_date > start_date)
);

-- Table: payments
CREATE TABLE payments (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id        UUID NOT NULL UNIQUE REFERENCES bookings(id),
  provider          VARCHAR(50) NOT NULL DEFAULT 'MOCK', -- 'MOCK' for now; real gateway name once integrated
  provider_ref      VARCHAR(255),               -- mock: generated fake ref; real gateway: its transaction id
  amount            NUMERIC(10,2) NOT NULL,
  status            VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCEEDED, FAILED, REFUNDED
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Table: access_codes
CREATE TABLE access_codes (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  booking_id  UUID NOT NULL UNIQUE REFERENCES bookings(id),
  code        VARCHAR(20) NOT NULL,
  issued_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  expires_at  TIMESTAMPTZ NOT NULL
);
```

## 4. Index Strategy
| Table | Index Name | Columns | Query Pattern |
|---|---|---|---|
| listings | idx_listings_city_status | (city, status) | Search: WHERE city = ? AND status = 'ACTIVE' |
| listings | idx_listings_location_gist | GIST(location) | Future radius/geo search |
| listings | idx_listings_owner | (owner_id) | Owner dashboard: WHERE owner_id = ? |
| bookings | idx_bookings_listing | (listing_id) | Availability check: WHERE listing_id = ? |
| bookings | idx_bookings_renter | (renter_id) | Renter dashboard: WHERE renter_id = ? |
| payments | idx_payments_booking | (booking_id) | 1:1 lookup (also enforced by UNIQUE) |

## 5. Migration Plan
| Migration File | Description | Reversible |
|---|---|---|
| V1__initial_schema.sql | users, roles, user_roles, listings, listing_photos, bookings, payments, access_codes + indexes | Yes (DROP TABLE in down migration) |
| V2__seed_roles.sql | Seed OWNER/RENTER/ADMIN into roles table | Yes |

(Flyway naming convention assumed — matches Spring Boot's default migration tool.)

## 6. Access Patterns
| Use Case | Query Pattern | Index Coverage |
|---|---|---|
| Search listings by city | SELECT ... WHERE city = ? AND status = 'ACTIVE' | idx_listings_city_status |
| Owner views their listings | SELECT ... WHERE owner_id = ? | idx_listings_owner |
| Check listing availability for date range | SELECT ... FROM bookings WHERE listing_id = ? AND status = 'CONFIRMED' AND daterange overlaps | idx_bookings_listing |
| Renter views their bookings | SELECT ... WHERE renter_id = ? | idx_bookings_renter |

## 7. Sensitive Data
- Columns requiring extra care: `users.password_hash` (BCrypt, never returned via API), `access_codes.code` (never logged, returned only to authorized parties), `payments.provider_ref` (treat as sensitive, avoid logging)
- Row-level security: not needed at MVP — enforced in the service layer via ownership checks (see Security Baseline). Revisit if direct DB access by multiple services is introduced later.
