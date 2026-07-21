# Security Baseline: Wida3.ma
**Architecture Reference**: docs/architecture-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Security Engineer

## 1. Threat Model (5-Minute)
- **What are we building?** A marketplace where warehouse owners list space and renters pay to book it, receiving an access code.
- **Who would attack it?** Opportunistic fraudsters (fake listings, stolen cards), competitors (scraping), and script kiddies (generic web attacks). No nation-state-level concern at this scale.
- **Worst outcome?** Financial fraud (fake booking/payment abuse), access-code leakage leading to unauthorized warehouse entry, or a data leak of user PII/payment metadata.

## 2. STRIDE Analysis (top risks only)
| Threat | Component | Mitigation | Status |
|---|---|---|---|
| Spoofing | Auth (login) | JWT with signed tokens, password hashing (BCrypt), rate-limit login attempts | TODO |
| Tampering | Booking/payment flow | Server-side price/availability recalculation on booking (never trust client price) | TODO |
| Repudiation | Booking/payment | Immutable booking + payment audit log (created_at, status transitions) | TODO |
| Info Disclosure | Access codes | Access code only returned to the confirmed renter and the listing owner; never logged in plaintext | TODO |
| DoS | Public search endpoint | Rate limiting at Nginx layer on `/api/v1/listings` | TODO |
| Elevation of Privilege | Role checks (Owner/Admin) | Resource-level ownership checks on every listing/booking mutation, not just role checks | TODO |

## 3. Authentication Strategy
- **Type**: JWT — short-lived access token (15 min) + httpOnly, Secure, SameSite=Strict refresh cookie (7 days)
- **MFA**: Not required for MVP (justify: low-risk consumer marketplace, not financial custody of funds beyond gateway-delegated payment) — revisit for Admin accounts specifically once live
- **Password policy**: Minimum 10 characters, checked against a common-password/breach list (e.g. Have I Been Pwned range API) at registration
- **Session management**: Access token in memory (not localStorage, to reduce XSS token-theft risk); refresh token httpOnly cookie; explicit logout invalidates refresh token server-side

## 4. Authorization Model
- **Pattern**: Simple RBAC — roles: OWNER, RENTER, ADMIN (a user can hold OWNER and RENTER simultaneously)
- **Roles defined**:
  - OWNER — manage own listings, view own bookings
  - RENTER — search, book, view own bookings
  - ADMIN — approve listings, view/manage all bookings
- **Resource-level checks**: Yes — every listing/booking mutation verifies the acting user owns the resource (or is ADMIN), enforced in the service layer, not just at the controller/route level

## 5. Data Protection
- **PII fields**: name, email, phone, address (owner warehouse address), payment metadata (last 4 digits only if stored — full card data never touches our DB, delegated to gateway once chosen)
- **Encryption at rest**: Database-level encryption via the hosting/VPS disk encryption; no application-level field encryption needed for MVP PII classes (revisit if storing ID documents/KYC data later)
- **Encryption in transit**: HTTPS enforced everywhere (Nginx TLS termination), HSTS enabled
- **Secrets management**: Environment variables injected via docker-compose `.env` (never committed) — see `.env.example` and DevOps doc for secrets handling in CI/CD

## 6. Security Requirements for Dev Team
- [ ] All inputs validated server-side (Bean Validation annotations on DTOs, never trust client-supplied price/availability)
- [ ] Output encoded for context (React escapes by default; avoid `dangerouslySetInnerHTML`)
- [ ] No secrets in code, logs, or error messages (access codes, tokens, and payment references must never appear in application logs)
- [ ] HTTPS only, security headers configured (HSTS, X-Content-Type-Options, X-Frame-Options, CSP baseline)
- [ ] Dependencies scanned in CI (SCA — see DevOps doc, Trivy for both Maven/npm dependencies and Docker images)

## Open Item
Payment is a mock gateway and file storage is local disk for MVP (see Architecture ADR-5/ADR-6). No real card data or webhook exists yet, so provider-specific requirements (e.g. webhook signature verification) don't apply. **Before accepting real payments or deploying beyond localhost**, this doc must be revisited to add: real gateway webhook verification, and access controls appropriate to whatever storage replaces local disk.
