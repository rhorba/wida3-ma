# RISKS — Wida3.ma

## 2026-07-22 — No JWT verification filter yet
- SecurityConfig's `.anyRequest().authenticated()` currently has no effect: there's no filter that reads the `Authorization` header, validates the JWT, and populates the SecurityContext. Not a problem for Story 1.1 (register/login are the only endpoints and are `permitAll`), but must be built before Story 1.2 or any protected endpoint (e.g. 2.1 Owner creates listing) ships, or those endpoints will incorrectly reject all requests (or worse, if misconfigured, accept all).

