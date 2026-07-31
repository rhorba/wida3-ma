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

## 2026-07-28 (continued) — Epic 3 Batch 3 complete
Implemented GET /api/v1/bookings unified list (also satisfies Epic 4 Story 4.1 admin-views-all-bookings): RENTER sees own bookings, OWNER sees bookings on their listings, a user with both roles sees the union (BookingRepository.findByRenterOrListingOwner), ADMIN sees every booking, ordered newest-first. No pagination (YAGNI, consistent with the earlier public-search decision).
Full backend suite: 65/65 tests passing (19 in BookingControllerIntegrationTest, 5 new for the list endpoint), 88% instruction coverage (JaCoCo, JDK 21). Coverage gate (>=80%) met.

## 2026-07-28 (continued) — Epic 3 Batch 4 complete
Frontend booking UI: SearchPage results now have an inline "Book" action (start/end date pickers -> POST /bookings, shows access code on success or the payment-failure reason on decline). New MyBookings component (dashboard) lists bookings via GET /bookings with a Cancel action (POST /bookings/{id}/cancel) for CONFIRMED bookings; DashboardPage bumps a remount key on successful booking so the list refreshes without a full page reload.
Verified against the real running app (backend on :8091, frontend on :5176, wida3-dev-postgres) rather than just typecheck/build, since the Chrome extension needed for a manual click-through wasn't connected this session: added a new Playwright e2e case (renter books an approved listing, sees the access code, cancels it) to frontend/e2e/critical-flows.spec.ts and ran the full e2e suite -- 3/3 passing. tsc --noEmit, vite build, and oxlint all clean (pre-existing AuthContext fast-refresh warning only). Dev servers stopped afterward; wida3-dev-postgres left running (disposable, per established convention).

## 2026-07-28 (continued) — Epic 3 Batch 5: security pass
Reviewed the two new endpoints (cancel, list) against the OWASP checklist (.claude/.skills/security-engineer/references/owasp-checklist.md):
- A01 Broken Access Control: both enforce auth server-side (SecurityConfig anyRequest().authenticated()) plus ownership checks in BookingService.requireViewAccess (renter/owner/admin only); covered by stranger-403 and cross-tenant-list-leak tests.
- A03 Injection: BookingRepository queries are JPQL with bound @Param placeholders, no string concatenation.
- Adversarial "bulk endpoint leaks cross-tenant data": GET /bookings scoping verified by test (a stranger's booking never appears in another renter's list).
Found and fixed one real gap: BookingService.cancel() read the booking via a plain findById, then checked status and issued a refund without holding a row lock -- two concurrent cancel calls on the same booking could both pass the CONFIRMED check and double-refund (TOCTOU race), unlike booking creation which already row-locks the listing (Comprehensive-tier decision, 2026-07-27). Fixed by adding BookingRepository.findByIdForUpdate (PESSIMISTIC_WRITE, mirrors the existing Listing lock) and using it in cancel(). Re-ran the full booking suite (20 tests) after the fix -- all green.
`npm audit --production` on frontend: 0 vulnerabilities.

## 2026-07-28 (continued) — Epic 3 Batch 5: version recording + risk log
Saved .recordings/v0.3.0-epic3-2026-07-28-renter-books-and-cancels.webm (renter searches -> books -> sees access code -> cancels, from the Playwright run against the live dev servers) per CLAUDE.md rule 9, marking Epic 3 + Epic 4 Story 4.1 as a completed project version (v0.3.0).
Logged the cancel double-refund race (found and fixed this session) to .logs/risks.md.

## 2026-07-28 (continued) — Epic 3 SHIP: push
Pushed all 5 Epic 3 commits (Batches 2-5) to origin/main. f593421..f89f289 main -> main. CI: no pipeline configured yet (open item, flagged repeatedly across sessions -- not a blocker per rule 11 since there is no pipeline to be red).

## 2026-07-28 (continued) — Mock payment gateway: realistic failure scenarios (Comprehensive)
Replaced the single hardcoded decline message with three distinct, deterministic failure scenarios triggered by the total booking price's fractional cents (no card-entry field exists anywhere in the app, so amount is the only per-request signal without adding new API surface): .13 -> "Insufficient funds", .66 -> "Card declined by issuer", .99 -> "Payment gateway timed out, please try again" (with a real ~300ms simulated delay). Any other amount succeeds; app.payment.mock.always-succeed=false still declines everything with the generic message, unchanged.
V7 migration adds payments.failure_reason; Payment entity carries it; BookingService now persists and surfaces the actual PaymentResult.failureReason instead of a hardcoded string (this was a latent bug -- the field existed in PaymentResult since Sprint 3 but was being discarded).
Added a QA mapping table to docs/architecture-wida3-ma.md next to ADR-5.
New tests: MockPaymentServiceImplTest (6 unit tests, one per branch) + 1 new booking integration test asserting the specific reason surfaces end-to-end. Full backend suite: 72/72 tests passing, 88% instruction coverage (JaCoCo, JDK 21). Gate (>=80%) met.

## 2026-07-31 — PLAN: Comprehensive CI pipeline
📋 BATCH 1: Foundation -- .github/workflows/ci.yml, backend job (setup-java 21 + mvn verify w/ dummy test env vars), frontend job (npm ci + oxlint + tsc/build)
📋 BATCH 2: Coverage gate (JaCoCo >=80%) + security scanning (gitleaks secrets, trivy fs SCA, Semgrep SAST)
📋 BATCH 3: Playwright e2e-in-CI (Postgres service, backend+frontend live, critical-flows.spec.ts, artifact upload)
📋 BATCH 4: Ship -- push to origin/main, gh run watch until green, log, ask user before any branch-protection change

## 2026-07-31 — EXECUTE Batch 1: CI foundation
Created .github/workflows/ci.yml with backend job (JDK 21 via setup-java, `mvn -B verify`) and frontend job (npm ci, oxlint, tsc+vite build). No env vars needed for the backend job -- confirmed all four integration test classes already supply spring.datasource.url/DB_USER/DB_PASSWORD/app.jwt.secret via @DynamicPropertySource against their own Testcontainers Postgres instance.

## 2026-07-31 (continued) — EXECUTE Batch 2: coverage gate + security scanning
Loaded .claude/.skills/devops-devsecops/references/cicd-security.md (rule 12, document-first) before writing the scan jobs; templates below match its Hardened Workflow Template.
Backend: added a `jacoco:check` execution to backend/pom.xml bound to the `verify` phase (BUNDLE/INSTRUCTION/COVEREDRATIO >= 0.80), so `mvn -B verify` in CI now fails the build if coverage drops below 80% -- closes CLAUDE.md rule 6 as an automated gate rather than a manual check. Verified locally with JDK 21 + the cached Maven 3.9.16 distribution (~/.m2/wrapper/dists): 72/72 tests, "All coverage checks have been met.", BUILD SUCCESS.
CI: added three jobs to .github/workflows/ci.yml -- secrets-scan (gitleaks/gitleaks-action@v2, full history via fetch-depth 0), sca (aquasecurity/trivy-action@master, fs scan, CRITICAL/HIGH, exit-code 1), sast (returntocorp/semgrep-action@v1, p/owasp-top-ten, security-events: write permission scoped to that job only). None of these three have run against live Actions yet -- first push may surface real findings (dependency CVEs, secret false-positives) that need triage; per rule 11 that's this session's responsibility to fix before SHIP, not a later one's.
Not pushed yet -- same holding pattern as Batch 1 (mandatory CI-watch obligation on push, plan is to push once through Batch 3 or at user's direction). Committed locally.

## 2026-07-31 (continued) — EXECUTE Batch 3: Playwright e2e-in-CI
Added an `e2e` job to .github/workflows/ci.yml, gated on `needs: [backend, frontend]`: a `postgres:16` service container (localhost:5432, throwaway creds), backend packaged via `mvn -B -DskipTests package` (tests already ran in the `backend` job) and started with `java -jar`, frontend started via `npx vite --port 5176 --strictPort` to match the Playwright config's default baseURL, both polled ready via curl loops (backend against the public GET /api/v1/listings/search endpoint, frontend against `/`) before `npm run test:e2e` runs. Uploads .recordings/raw as a build artifact (`if-no-files-found: warn` so a pre-e2e failure doesn't itself fail the job).
Found and fixed a real portability bug while wiring this up: frontend/e2e/critical-flows.spec.ts hardcoded `docker exec wida3-dev-postgres psql ...` to promote a test user to ADMIN -- this only works against the named local dev container and would have failed outright against CI's plain service container. Refactored into a `promoteToAdmin()` helper reading the psql invocation from a new `E2E_DB_EXEC` env var (default unchanged: the local docker-exec command), with CI setting `E2E_DB_EXEC` to a direct TCP `psql -h localhost -p 5432 ...` plus `PGPASSWORD` -- one code path, both environments. Verified with `npm run lint` (oxlint, clean except the pre-existing AuthContext fast-refresh warning) and `npm run build` (tsc+vite, clean); the e2e job itself has not run against live Actions yet since nothing has been pushed.
Still committed locally, not pushed -- Batch 4 (push + `gh run watch` to green) is next.

## 2026-07-31 (continued) — SHIP: push + CI red -> fix -> repush
Pushed all 4 CI-pipeline commits (cd0efc6..0ff7613) to origin/main. First live run (30609998909): backend, frontend, secrets-scan, sast all green; sca and e2e both red. Per rule 11, fixed both before treating anything as shipped:
- sca (Trivy fs scan): failed with a Maven Central 429 (spring-batch-bom parent POM lookup, rate-limited on the shared GH Actions runner IP) -- Trivy needs local .m2 to resolve pom.xml's effective dependency tree and had none on a cold runner. Fix: added `needs: [backend]` plus the same setup-java/cache:maven step, so the sca job reuses the Maven cache the backend job already populated -- no network POM resolution needed.
- e2e (Playwright): all 3 tests failed at the first "Register" step (heading never appeared). Root cause: the frontend defaults VITE_API_BASE_URL to `http://localhost:8080/api/v1` (client.ts) when unset, which is a real cross-origin call from the browser (frontend :5176, backend :8080) -- and the backend has no CORS config, so the browser silently blocked it (curl's readiness check didn't catch this since curl isn't subject to CORS). Fix: set `VITE_API_BASE_URL=/api/v1` when starting the frontend in CI, routing calls through Vite's dev proxy (vite.config.ts) as a same-origin request -- mirrors the workaround already used for local manual testing (2026-07-27 session note). Flagging for later: this is a real gap for any actual multi-origin deployment (no reverse proxy), not just a CI quirk -- out of scope to fix now (Epic 5 / deployment readiness territory), noted here so it isn't lost.
Not yet re-pushed -- about to commit both fixes and re-run.

## 2026-07-31 (continued) — Second CI run: e2e green, sca red for a real reason
Pushed the sca-cache + e2e-CORS fixes (1f7f723). Second run (30610386314): backend, frontend, secrets-scan, sast, e2e all green. sca still red -- but this time correctly, not a tooling problem: Trivy found 30 real CRITICAL/HIGH CVEs (Jackson-databind RCE, several Spring Framework/Security/WebMVC and Tomcat CVEs) in transitive dependencies pulled in by the pinned spring-boot-starter-parent 3.3.4. Stopped and presented this to the user as a blocker rather than silently picking a fix, per CLAUDE.md's Blocker Protocol -- a dependency upgrade is real scope, not a CI config tweak. User chose a same-minor-line patch bump (3.3.4 -> 3.3.13, latest available 3.3.x per Maven Central metadata) over jumping to 3.5.16 (lower API-compat risk) or deferring with a .trivyignore.
Bumped backend/pom.xml parent version to 3.3.13. Verified locally (JDK 21, cached Maven 3.9.16): 72/72 tests still pass, JaCoCo coverage gate still met, BUILD SUCCESS. All 30 findings were backend-only (pom.xml) -- npm/package-lock.json had none.

## 2026-07-31 (continued) — Third CI run: sca still red, for real -- revised to 3.5.16
Pushed the 3.3.13 bump (04968c4). Third run (30616125335): everything green except sca, which dropped from 30 to 24 findings (4 CRITICAL, 20 HIGH) but couldn't go further -- traced the remaining CRITICAL Spring Boot finding (CVE-2026-40973) to a fix that only exists at 3.5.14+/4.0.6, meaning no 3.3.x release can ever close it; the earlier "low-risk patch bump" premise the user approved no longer held once this was known.
Rather than silently re-deciding, tested the real fix directly before going back to the user: bumped backend/pom.xml to spring-boot-starter-parent 3.5.16 locally (two minor versions up) and ran the full suite -- clean drop-in, 72/72 tests passed with zero code changes, coverage gate met. Presented this updated picture (patch bump insufficient; minor-version jump verified clean) to the user, who approved switching to 3.5.16 over the alternative (defer via .trivyignore). `mvn dependency:tree` confirms every previously-flagged library now resolves at/above its fixed version: spring-boot 3.5.16, tomcat-embed-core 10.1.55, spring-security-web 6.5.11, spring-webmvc/core/expression 6.2.19, postgresql 42.7.11, jackson-databind 2.21.4.

## 2026-07-31 (continued) — Fourth CI run: down to 1 finding, closed
Pushed the 3.5.16 bump (0ee60ef). Fourth run (30617146559): every job green except sca, now down to a single HIGH (CVE-2026-54291, SCRAM-SHA-256-PLUS downgrade in org.postgresql:postgresql 42.7.11, fixed in 42.7.12) -- an isolated JDBC-driver patch with no framework coupling, unlike the earlier findings. Overrode `<postgresql.version>42.7.12</postgresql.version>` in backend/pom.xml's properties (Spring Boot's managed-dependency override mechanism). Verified locally: dependency:tree confirms 42.7.12 resolves, 72/72 tests still pass, coverage gate met.
Pushed (3c5204f). Fifth run (30617609510): all 6 jobs green (backend, frontend, secrets-scan, sast, sca, e2e). CI pipeline (Comprehensive plan, all 4 batches) is fully shipped and closes the open item flagged repeatedly since 2026-07-27. Working tree clean except the pre-existing untracked bash.exe.stackdump (not from this session, left as-is).
