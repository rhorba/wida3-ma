# PRD: Wida3.ma — Warehouse Marketplace
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: PM | **Status**: Draft

## 1. Problem Statement
Morocco's SMEs (transport operators, agri buyers, retailers) rent expensive long-term storage they don't fully use, while warehouse owners sit on dead capacity with no easy way to monetize it. No platform matches short-term storage demand with unused warehouse supply.

## 2. Goals & Success Metrics
| Goal | Metric | Target (MVP) |
|---|---|---|
| Owners can list space fast | Time to publish a listing | < 5 min |
| Renters can find & book fast | Time from search to confirmed booking | < 3 min |
| Marketplace has liquidity | Listings live at MVP launch | ≥ 20 warehouses (Casablanca + Rabat) |
| Bookings actually happen | Confirmed bookings in first 60 days | ≥ 30 |
| Handoff works without friction | Bookings requiring manual support intervention | < 15% |

## 3. User Stories
As a **Warehouse Owner**, I want to list my unused space with photos, size, type, and price, so that I can earn revenue from idle capacity.
As a **Warehouse Owner**, I want to set availability and pricing by week, so that I control how my space is booked.
As a **Business Renter**, I want to search warehouses by city, type, and size, so that I can find storage that fits my need.
As a **Business Renter**, I want to book by the week and pay online, so that I can secure space without a phone call.
As a **Business Renter**, I want an access code issued automatically on booking confirmation, so that I can access the space without meeting the owner.
As an **Admin**, I want to review and approve new listings, so that the marketplace maintains quality and trust.
As an **Admin**, I want to see all bookings and flag disputes, so that I can support both sides when something goes wrong.

## 4. Scope
### In Scope
- Warehouse listing creation & management (owner)
- Search & filter by city, type, size (renter)
- Weekly booking with online payment (renter)
- Auto-generated access code on confirmed booking
- Basic insurance disclosure (informational, not underwritten in MVP)
- Admin listing approval + booking oversight

### Out of Scope (MVP)
- Real-time chat between owner and renter
- Dynamic/algorithmic pricing
- Native mobile apps (web-responsive only)
- Multi-day (sub-week) bookings
- Automated insurance underwriting/claims
- Multi-language i18n beyond French/Arabic labels in UI copy

## 5. Requirements
### Functional
- FR-1: Owner can create/edit/deactivate a warehouse listing (city, address, type, size in m², weekly price, photos, availability calendar)
- FR-2: Renter can search listings by city, type, size range, and available date range
- FR-3: Renter can book an available listing for one or more consecutive weeks
- FR-4: System processes payment for a booking before confirming it
- FR-5: System generates and delivers a unique access code to the renter on booking confirmation
- FR-6: Admin can approve/reject new listings and view all bookings
- FR-7: Owner and renter each have a dashboard showing their listings/bookings and status

### Non-Functional
- NFR-1: Performance — search results return in < 1s p95 for MVP data volume
- NFR-2: Security — all payment and PII data encrypted in transit (HTTPS) and at rest for sensitive fields
- NFR-3: Accessibility — WCAG 2.1 AA for core flows (search, listing, booking)
- NFR-4: Availability — 99% uptime target for MVP (single-region, single VPS)

## 6. Constraints & Assumptions
- Team is building a fresh backend in Java/Spring Boot (pivot from earlier Next.js prototype scope) — no legacy code to migrate.
- Deployment target is a single VPS via Docker/docker-compose (YAGNI — no Kubernetes at this stage).
- Payment gateway and file storage provider are **not yet decided** — flagged as an open decision before those specific stories are built.
- Initial launch market: Casablanca and Rabat only.

## 7. Risks
| Risk | Probability | Impact | Mitigation |
|---|---|---|---|
| Not enough warehouse owners list at launch (cold start) | M | H | Manually onboard first ~20 owners before public launch |
| Payment gateway integration (CMI or alternative) slips | M | M | Decide gateway early in Sprint 2, before booking-payment story starts |
| Access-code handoff fails in the field (no physical verification) | M | M | Add manual admin override / support contact on every booking |
| Geo search (PostGIS) adds complexity for MVP scale | L | M | Start with simple city-match filter; add radius/geo search only if search-by-distance is requested |

## 8. Timeline
| Milestone | Target Date |
|---|---|
| PRD Approved | 2026-07-21 |
| Architecture Done | 2026-07-21 (Sprint 1) |
| Foundation docs pushed | 2026-07-21 |
| Implementation Start (Sprint 2) | TBD — next session |
| MVP Ready | TBD — to be set once Sprint 2 backlog is sized |
