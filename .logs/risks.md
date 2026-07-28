# RISKS — Wida3.ma

## 2026-07-22 — No JWT verification filter yet — RESOLVED in Story 1.2
- SecurityConfig's `.anyRequest().authenticated()` currently has no effect: there's no filter that reads the `Authorization` header, validates the JWT, and populates the SecurityContext. Not a problem for Story 1.1 (register/login are the only endpoints and are `permitAll`), but must be built before Story 1.2 or any protected endpoint (e.g. 2.1 Owner creates listing) ships, or those endpoints will incorrectly reject all requests (or worse, if misconfigured, accept all).
- Resolved: `JwtAuthFilter` added and wired into `SecurityConfig` in Story 1.2, verified live (valid token passes the security layer, missing/invalid token returns 401).


## 2026-07-28 — Booking cancel double-refund race (found & fixed same session)
Risk: BookingService.cancel() checked booking status and issued a refund without a row lock, so two concurrent cancel calls on the same booking could both pass and double-refund. Severity: low in current state (mock payment gateway, no real money), but would become Medium/High once Story 5.1 swaps in a real payment provider.
Mitigation: fixed this session via BookingRepository.findByIdForUpdate (PESSIMISTIC_WRITE) mirroring the existing listing row-lock. No outstanding action needed unless Story 5.1's real-gateway integration reveals gateway-side idempotency gaps too.
