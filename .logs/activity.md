# ACTIVITY — Wida3.ma



## 2026-07-21 — Sprint 1: Foundation docs (PLAN/EXECUTE)
- 📝 MILESTONE: PRD drafted → docs/prd-wida3-ma.md
- 📝 MILESTONE: System Design drafted → docs/system-design-wida3-ma.md
- 📝 MILESTONE: Architecture drafted → docs/architecture-wida3-ma.md
- 📝 MILESTONE: Security Baseline drafted → docs/security-wida3-ma.md
- 📝 MILESTONE: Database Design drafted → docs/database-wida3-ma.md
- 📝 MILESTONE: UX Foundation drafted → docs/ux-wida3-ma.md
- 📝 MILESTONE: UI Foundation drafted → docs/ui-wida3-ma.md
- 📝 MILESTONE: Test Strategy drafted → docs/test-strategy-wida3-ma.md
- 📝 MILESTONE: DevOps Foundation drafted → docs/devops-wida3-ma.md
- 📝 MILESTONE: Stories drafted → docs/stories-wida3-ma.md
- .env.example written with all identified env vars (DB, JWT, payment TBD, storage TBD, frontend API base URL)
- README.md updated to reflect stack pivot
- Status: awaiting user approval (VERIFY gate) before commit + push (SHIP)

## 2026-07-21 — SHIP
- 📝 PUSH: commit e8e3865 "docs: foundation documents for wida3-ma" → origin/main (https://github.com/rhorba/wida3-ma)
- CI monitoring: N/A this sprint (no CI pipeline configured yet — CI pipeline itself is defined in docs/devops-wida3-ma.md, to be wired up when Sprint 2 code lands)

## 2026-07-21 — Doc correction SHIP
- 📝 PUSH: commit c5d7dca "docs: resolve Sprint 1 open items" → origin/main

## 2026-07-22 — Housekeeping
- 📝 MILESTONE: relocated skills/ → .claude/.skills/ (confirmed intentional by user) and updated all skills/... path references in CLAUDE.md to .claude/.skills/... → commit 8e8c71e (not yet pushed, no sprint boundary crossed)

## 2026-07-22 — Sprint 2, Story 1.1 (Backend Dev) — Batch 1: scaffold
- Created backend/ Maven project (Spring Boot 3.3.4, Java 21) with Web, Data JPA, Security, Validation, Postgres, Flyway, JWT (jjwt), bucket4j (rate limiting), Testcontainers, JaCoCo
- V1__initial_schema.sql: users (incl. failed_attempts/locked_until for the comprehensive lockout requirement — not in original DBA doc, added for this story's scope), roles, user_roles; V2__seed_roles.sql seeds OWNER/RENTER/ADMIN
- users/roles JPA entities + repositories wired
- application.yml wired to .env.example vars (JWT secret/TTL, datasource, lockout threshold)
- Scope note: only auth tables created now (not full V1 schema from Database doc) — listings/bookings/etc. tables deferred to their own stories' migrations, per YAGNI

## 2026-07-22 — Sprint 2, Story 1.1 — Batch 2+3: core logic, comprehensive extras, tests
- AuthService: register (breach-list check via HIBP k-anonymity, EmailAlreadyRegistered/BreachedPassword errors) + login (BCrypt verify, JWT issuance, failed-attempt counter, account lockout after 5 attempts/15min)
- JwtService (HMAC JWT issue/verify), AuthRateLimiter (per-IP bucket4j, 10 req/min on /auth/*), GlobalExceptionHandler (no internal leakage)
- 📝 MILESTONE: VERIFY passed — 16/16 tests, 80% coverage, security self-check clean (see .logs/metrics.md for detail). One real bug found+fixed: transactional rollback was silently defeating the lockout feature.
- Committed locally: 2b1be09 (not pushed — holding until Sprint 2's other stories — 1.2, 2.1 — land, matching Sprint 1's single-push-at-sprint-end pattern)

## 2026-07-22 — Sprint 2, Story 1.2 (Backend Dev + Frontend Dev): JWT refresh & logout
- Backend: refresh_tokens table (V3 migration, opaque token stored as SHA-256 hash), RefreshTokenService (issue/rotate/revoke, reuse-detection revokes all active tokens for that user), JwtAuthFilter + SecurityConfig wiring (closes the risk logged after 1.1), /auth/refresh + /auth/logout endpoints, refresh cookie (httpOnly/Secure-configurable/SameSite=Strict) added to register+login responses
- Frontend: Vite + React 19 + TypeScript scaffold under frontend/, API client with silent-refresh-on-401 retry, AuthContext (access token in memory only, per Security Baseline §3), minimal Login/Register/Dashboard pages
- 📝 MILESTONE: VERIFY passed — 24/24 tests, 84% coverage, live manual E2E verification via curl (browser extension unavailable this session). Three real bugs found via live testing and fixed: 500-instead-of-405 on wrong HTTP method, 500-instead-of-404 on unmatched paths, 403-instead-of-401 on unauthenticated requests (this last one would have silently broken the frontend's silent-refresh feature — the whole point of this story). See .logs/metrics.md for detail.
- Committed locally (not pushed yet, same sprint-end-push pattern as 1.1)

## 2026-07-22 — Sprint 2, Story 2.1 (Backend Dev + Frontend Dev): Owner creates a listing
- Owner-role gap closed first: /auth/register now accepts an optional `roles` field (self-select OWNER in addition to default RENTER; ADMIN never self-assignable) — frontend RegisterPage got an "I want to list a warehouse" checkbox
- Backend: listings + listing_photos tables (V4 migration, PostGIS location column deferred — see .logs/decisions.md), FileStorageService (content-type/size validated, random-filename storage, no path traversal possible), POST /api/v1/files/upload + POST /api/v1/listings (both @PreAuthorize("hasRole('OWNER')")), static resource handler serving /uploads/** for local dev, @EnableMethodSecurity added
- Frontend: CreateListingForm (fields + file picker + photo previews, uploads then creates), wired into DashboardPage (visible only when the logged-in user holds OWNER)
- 📝 MILESTONE: VERIFY passed — 36/36 tests, 86% coverage, security self-check clean. See .logs/metrics.md for detail, including the judgment call to rely on integration tests rather than repeating manual live-server verification this story.
- Committed locally (not pushed yet)
- This was the last of the three stories planned for Sprint 2 (1.1, 1.2, 2.1) — sprint-end push still pending, per CLAUDE.md rule 7.

## 2026-07-23 — Sprint 2 SHIP: Playwright recording + push
- Docker Desktop verified healthy (was down at session start); wida3-dev-postgres container had survived the previous session's incident as stopped (not lost as feared) — restarted with original data/credentials
- Backend run locally via a cached Maven 3.9.9 distribution found under `~/.m2/wrapper/dists` (no system `mvn`/`mvnw` available) on port 8091 (8080 was occupied by an unrelated project's auto-restarted container — left untouched per the prior session's port-kill incident)
- Added a dev-only Vite proxy (`/api` → `VITE_DEV_API_PROXY_TARGET`) in `frontend/vite.config.ts` so the Vite dev server and backend can run cross-port locally without CORS — no backend change needed
- Recorded `.recordings/v0.1.0-sprint2-2026-07-23.webm` via a new Playwright spec (`frontend/e2e/critical-flows.spec.ts`): register (as Owner) → create listing with photo upload → logout → login. Fulfills CLAUDE.md rule 9 for Sprint 2.
- Real bug found (not fixed, logged for backlog): `RegisterPage.handleSubmit` has no `try/finally` around `await register(...)` — an unhandled fetch rejection leaves the submit button stuck on "Registering..." forever. Only surfaced because of a local CORS misconfiguration during this session's debugging, but the missing error handling is real and independent of that.
- 📝 PUSH: commit 5210fb2 "test(e2e): add Playwright critical-flow recording for Sprint 2" (plus the three story commits 2b1be09, 4e4fbbb, 478c2e6) → origin/main. Sprint 2 SHIP complete.
- CI monitoring: N/A — no CI pipeline configured yet (same as Sprint 1)

## Bug fix — 2026-07-26
Fixed stuck-submit-button bug (logged 2026-07-23): RegisterPage.handleSubmit and LoginPage.handleSubmit awaited register()/login() without try/finally, so a network-level fetch rejection (not just an HTTP error response) left setSubmitting(true) forever. Added try/catch/finally to both, matching the pattern already used in CreateListingForm. Verified via `npm run build` (tsc + vite) and `npm run lint` (oxlint) -- both clean; no frontend unit-test framework exists yet (Playwright e2e only) so no regression test added for this micro-fix.

## PUSH — 2026-07-26
Pushed bug-fix commit c1040be to origin/main (9bbb34c..c1040be).

## Batch 1+2 complete — 2026-07-27
Story 2.2 (public search) + Story 2.3 (admin approve/reject) backend done: GET /api/v1/listings/search (public, exact-match city/type/size-range, ACTIVE-only), GET /api/v1/listings/pending + PATCH .../approve + PATCH .../reject (ADMIN-only, reject requires a reason, persisted via new V5 migration column). Domain-level approve()/reject() on Listing enforce PENDING_APPROVAL-only transitions (409 otherwise), 404 on unknown id. Environment note: system default JDK is now 25 (JaCoCo 0.8.12 + Mockito inline mock maker both incompatible with it) -- had to run tests explicitly under the separately-installed JDK 21 (project target) via JAVA_HOME override; worth fixing at the environment level eventually but out of scope here.

## Batch 3 complete + manual verification — 2026-07-27
Frontend: SearchPage (public, filters by city/type/size range, reachable pre-login and from dashboard) and AdminApprovalQueue (ADMIN-only, approve/reject with required reason) wired into App.tsx/DashboardPage.tsx. Fixed a real bug caught by the existing Playwright spec: SearchPage duplicated the "City" field label used by CreateListingForm, breaking getByLabel lookups once both render together on the dashboard -- renamed to "Location". Playwright critical-flow e2e re-passes. Manually verified in a real Chrome tab via claude-in-chrome: registered/promoted a DB-level admin, approved one pending listing (queue count 9->8), confirmed it then appears in public search, rejected another with a reason (queue count 8->7). Two dev-environment gotchas hit while manually testing (session-local, not code changes): (1) forgot VITE_API_BASE_URL when starting Vite so requests bypassed the dev proxy entirely; (2) Git Bash/MSYS auto-mangles env var values that look like absolute paths (e.g. "/api/v1" became a file:// Windows path) -- needed MSYS_NO_PATHCONV=1. Worth a one-line note in README dev-setup if this comes up again.

## PUSH — 2026-07-27
Pushed Sprint 3 Epic 2 commit 6b707c4 to origin/main (68d29a6..6b707c4).

## VIDEO_RECORDED — 2026-07-27
Extended frontend/e2e/critical-flows.spec.ts with a second test covering the new Epic 2 flows: owner creates two pending listings, an admin account is provisioned (registered then promoted to ADMIN directly in the dev Postgres, since ADMIN is intentionally not self-assignable), admin approves one listing and rejects the other with a reason, then public search confirms only the approved listing is visible. Recorded both critical-flow tests per CLAUDE.md rule 9: .recordings/v0.2.0-epic2-2026-07-27-owner-register-list-login.webm and .recordings/v0.2.0-epic2-2026-07-27-admin-approve-reject-search.webm. Added .recordings/raw/ to .gitignore (Playwright per-test debug artifacts, not the deliverable).

## PUSH — 2026-07-27
Pushed e2e extension + recordings commit 2d835fb to origin/main (6b707c4..2d835fb). Epic 2 (Stories 2.1, 2.2, 2.3) fully shipped, verified, and recorded.

## Batch 1 complete — 2026-07-28
Story 3.1 (renter books available weeks) + Story 3.2 (access code on confirmed booking) backend done: V6 migration adds bookings/payments/access_codes tables plus a btree_gist EXCLUDE constraint (no two CONFIRMED bookings may overlap on the same listing) and a nullable unique idempotency_key column. Booking/Payment/AccessCode entities (Payment+AccessCode cascaded 1:1 off Booking, like ListingPhoto off Listing). PaymentService interface + MockPaymentServiceImpl (outcome controlled by app.payment.mock.always-succeed, defaults true, overridable in tests/env). POST /api/v1/bookings: row-locks the listing (findByIdForUpdate), rejects non-ACTIVE listings, validates whole-week date ranges, rejects overlapping CONFIRMED bookings before attempting payment, honors an optional Idempotency-Key header (replays the original result on retry, including under a DB-level race via catching DataIntegrityViolationException). GET /api/v1/bookings/{id} restricted to the renter, the listings owner, or an admin (403 otherwise). 54/54 backend tests passing (9 new), 87% coverage. Verified with two full suite runs -- first run hit a transient Testcontainers Postgres startup timeout (Docker resource contention under back-to-back container launches), not a code issue; second run was clean.
Remaining Epic 3 (Comprehensive scope, user-approved) not yet started: Batch 2 cancellation/refund, Batch 3 unified GET /api/v1/bookings list (also covers Epic 4 Story 4.1), Batch 4 frontend booking UI, Batch 5 verify+ship. Nothing pushed yet this session -- committing Batch 1 locally now per the established pattern of committing per-batch and pushing at a natural checkpoint.

## 2026-07-28 (continued) — PUSH
Pushed Epic 3 Batch 1 (d51a867 feat(bookings), f593421 chore) to origin/main. ce9ba98..f593421 main -> main.

## 2026-07-28 (continued) — Epic 3 Batch 2 complete
Implemented POST /api/v1/bookings/{id}/cancel: renter/owner/admin can cancel a CONFIRMED booking (403 for strangers, same access rule as GET), triggers PaymentService.refund + Payment.markRefunded when payment had succeeded, revokes the access code (orphanRemoval on Booking.accessCode), rejects cancelling a non-CONFIRMED booking with 409 INVALID_BOOKING_STATE. Added InvalidBookingStateException + GlobalExceptionHandler mapping.
Full backend suite: 60/60 tests passing (14 in BookingControllerIntegrationTest, 6 new for cancel), 88% instruction / 77% branch coverage (JaCoCo, JDK 21). Coverage gate (>=80%) met.
