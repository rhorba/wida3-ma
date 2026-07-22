# METRICS — Wida3.ma



## 2026-07-21 — Sprint 1 VERIFY
- No application code written this sprint (docs-only, per CLAUDE.md rule 13 — foundation docs before code).
- Test suite / coverage gate: N/A this sprint, applies starting Sprint 2 once code exists.
- Security check: doc content reviewed for internal consistency (payment/storage TBD flagged consistently across Architecture/Security/DevOps/Stories); no code to scan.

## 2026-07-22 — Sprint 2, Story 1.1 VERIFY
- Tests: 16/16 passing (7 AuthServiceTest unit, 2 JwtServiceTest unit, 2 AuthRateLimiterTest unit, 5 AuthControllerIntegrationTest Testcontainers/Postgres integration)
- Coverage (JaCoCo, instructions): 80% (638/792) — meets the ≥80% gate
- Coverage by package: auth.dto 100%, auth.exception 100%, auth.service 97%, auth.controller 87%, auth.entity 89%, common.exception 75%, auth.security 60%, com.wida3 (main class) 37% (untested entrypoint, acceptable)
- Security check: no plaintext password logging, generic error responses (no internal leakage), HIBP breach check only ever transmits a SHA-1 prefix (k-anonymity), registration cannot self-assign OWNER/ADMIN roles. Gap logged to .logs/risks.md: no JWT verification filter yet (fine for this story, blocks Story 1.2 / protected endpoints).
- Bug caught by testing and fixed: login()'s @Transactional method was rolling back the failed-attempt counter and lockout on every wrong-password attempt (Spring's default rollback-on-RuntimeException behavior) — the lockout feature was a silent no-op until fixed with @Transactional(noRollbackFor = InvalidCredentialsException.class).
