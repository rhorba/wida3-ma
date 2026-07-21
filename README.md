# Wida3.ma — Warehouse Marketplace

Morocco's SMEs rent expensive long-term storage they don't fully use. Warehouse owners have dead capacity.

## Problem
No platform matches businesses needing flexible short-term storage with warehouse owners who have unused space.

## Solution
Airbnb for warehouses — search by city/type/size, book by the week, integrated insurance and access-code handoff.

## Stack
Java 21 + Spring Boot 3 (backend), React + TypeScript (frontend), PostgreSQL 16 + PostGIS, Docker/docker-compose (deployment). Payment gateway and object storage provider: TBD (see `docs/system-design-wida3-ma.md`).

Full foundation docs: `docs/prd-wida3-ma.md`, `system-design-`, `architecture-`, `security-`, `database-`, `ux-`, `ui-`, `test-strategy-`, `devops-`, `stories-wida3-ma.md`.

## Completes
naql (transport SMEs need affordable storage) + Mawsim (agri buyers need receiving warehouses)

## Key Roles
Warehouse Owner | Business Renter | Admin
