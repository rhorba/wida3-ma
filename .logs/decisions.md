# DECISIONS — Wida3.ma



## 2026-07-21 — Stack pivot & scope
- Stack changed from README's Next.js/Drizzle to: Backend Java 21 + Spring Boot 3, Frontend React + TypeScript, DB PostgreSQL + PostGIS, Docker/docker-compose deployment on a single VPS.
- Frontend framework: React chosen over Angular (YAGNI — user picked React).
- Payment (CMI) and file storage provider: deferred/TBD, to be decided before those features are built.
- Git remote confirmed: https://github.com/rhorba/wida3-ma (no local .git yet — will init this session).
- Sprint 1 scope: full foundation doc set, no code.

## 2026-07-21 — Sprint 1 open items resolved
- Mock payment gateway, local disk file storage, localhost-as-VPS deployment — all confirmed by user. Real gateway/object storage/VPS integration deferred to a pre-launch Epic 5 (stories 5.1, 5.2).

## 2026-07-22 — Story 1.1 approach
- User chose 🔴 COMPREHENSIVE over 🟡 BALANCED (recommended) for Story 1.1 (registration/login): scaffold + BCrypt + breach-list password check + integration tests + rate-limiting on auth endpoints + account lockout after failed attempts. Lockout/rate-limiting go beyond the story's written acceptance criteria but were explicitly requested.

## 2026-07-22 — Story 1.2 scope
- User chose "full stack now" over "backend only" for Story 1.2 (JWT refresh & logout): also scaffolding the React + TypeScript frontend project this session, rather than deferring it to a separate story/session.

## 2026-07-22 — Owner role assignment (gap found ahead of Story 2.1)
- No story previously covered how a user gets the OWNER role (Story 1.1 only auto-assigns RENTER). User chose: let registration pick role(s) — an optional `roles` field on /auth/register lets a user self-select OWNER in addition to the default RENTER. ADMIN is never self-selectable (security boundary — admin remains assigned out-of-band only).

## 2026-07-22 — Story 2.1 approach + schema deviation
- User chose 🔴 COMPREHENSIVE for Story 2.1 (Owner creates a listing): listing creation + file upload with content-type/size validation + max-photo-count limit + frontend Create Listing form + full integration test coverage.
- Deviation from Database doc: deferring the `listings.location GEOGRAPHY(POINT,4326)` PostGIS column — no Postgres instance in this project runs the PostGIS extension yet, and this story's acceptance criteria don't need geo-search (that's a later story). Column will be added via its own migration when geo-search is actually built, matching the incremental-migration pattern already agreed for the auth tables.

## Decision — 2026-07-27
Sprint 3 scope: Epic 2 completion (Stories 2.2 public search, 2.3 admin approve/reject). Approach chosen: simple unpaginated exact-match search (city/type/size range), plus a persisted reject reason (new `rejection_reason` column) on the admin reject action -- rejected the fully "Balanced" option (no pagination yet, YAGNI) and "Comprehensive" (geo-radius search deferred, DB doc already flags it as future work).
