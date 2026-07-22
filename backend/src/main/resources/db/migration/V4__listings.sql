CREATE TABLE listings (
  id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id       UUID NOT NULL REFERENCES users(id),
  title          VARCHAR(255) NOT NULL,
  city           VARCHAR(100) NOT NULL,
  address        VARCHAR(500) NOT NULL,
  warehouse_type VARCHAR(50) NOT NULL,
  size_sqm       NUMERIC(10,2) NOT NULL,
  weekly_price   NUMERIC(10,2) NOT NULL,
  status         VARCHAR(20) NOT NULL DEFAULT 'PENDING_APPROVAL',
  created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_listings_owner ON listings(owner_id);
CREATE INDEX idx_listings_city_status ON listings(city, status);

CREATE TABLE listing_photos (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  listing_id  UUID NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
  file_url    VARCHAR(1000) NOT NULL,
  sort_order  SMALLINT NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
