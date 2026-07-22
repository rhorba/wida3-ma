CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Table: users
CREATE TABLE users (
  id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email             VARCHAR(255) NOT NULL UNIQUE,
  password_hash     VARCHAR(255) NOT NULL,
  full_name         VARCHAR(255) NOT NULL,
  phone             VARCHAR(30),
  failed_attempts   SMALLINT NOT NULL DEFAULT 0,
  locked_until      TIMESTAMPTZ,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
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
