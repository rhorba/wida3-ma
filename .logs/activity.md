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
