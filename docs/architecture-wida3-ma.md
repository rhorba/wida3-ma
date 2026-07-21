# Architecture: Wida3.ma
**PRD Reference**: docs/prd-wida3-ma.md
**System Design Reference**: docs/system-design-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: Tech Lead

## 1. Overview
A modular-monolith Spring Boot API backed by PostgreSQL/PostGIS, serving a React SPA, deployed together via Docker Compose on a single VPS. Module boundaries mirror the PRD's domains: auth, listings, booking, payment, files.

## 2. Architecture Decision Records

### ADR-1: Backend framework — Java 21 + Spring Boot 3
- **Context**: User specified Java Spring stack for this pivot.
- **Decision**: Spring Boot 3.x on Java 21 (LTS), using Spring Web (REST), Spring Data JPA, Spring Security.
- **Alternatives**: Quarkus/Micronaut — rejected, Spring Boot has the largest ecosystem/community and matches the explicit stack request.
- **Consequences**: Standard layered structure (controller → service → repository); Gradle or Maven build; Docker image built from a Spring Boot fat jar.

### ADR-2: Frontend framework — React + TypeScript
- **Context**: User asked for "Angular React"; YAGNI requires picking one. User confirmed React.
- **Decision**: React 18+ with TypeScript, Vite as build tool, React Router for routing.
- **Alternatives**: Angular — rejected per user decision this session (see .logs/decisions.md).
- **Consequences**: SPA consuming the Spring Boot REST API over JSON; no server-side rendering in MVP (not required — internal marketplace app, SEO not a driver for logged-in flows; public listing pages can add SSR later if SEO becomes a priority).

### ADR-3: Modular monolith, not microservices
- Inherited from System Design SDR-1. Module boundaries enforced via Java package structure (`com.wida3.auth`, `.listings`, `.booking`, `.payment`, `.files`), not separate deployables.

### ADR-4: Authentication — JWT-based session
- **Context**: SPA + REST API needs stateless auth that works cleanly behind Nginx/Docker.
- **Decision**: Spring Security with JWT access tokens (short-lived) + refresh token (httpOnly cookie).
- **Alternatives**: Server-side sessions — rejected, adds sticky-session complexity with no current multi-instance need; JWT is simpler for a single API instance and still gives a clean path to scale out later.
- **Consequences**: Frontend stores access token in memory, refresh token in httpOnly cookie; see Security Baseline for details.

## 3. System Design
```
[React SPA] → [Nginx] → [Spring Boot API]
                              ├── AuthController → AuthService → UserRepository → [Postgres]
                              ├── ListingController → ListingService → ListingRepository → [Postgres+PostGIS]
                              ├── BookingController → BookingService → BookingRepository → [Postgres]
                              │                                   ↓
                              │                          PaymentService → [Payment Gateway — TBD]
                              └── FileController → FileService → [Object Storage — TBD]
```

## 4. Data Model
```
User ──1:N──> Listing            (owner relationship)
User ──1:N──> Booking            (renter relationship)
Listing ──1:N──> Booking
Listing ──1:N──> ListingPhoto
Booking ──1:1──> Payment
Booking ──1:1──> AccessCode
User ──N:N──> Role (via user_roles: OWNER, RENTER, ADMIN)
```
(Full schema handed off to DBA — see docs/database-wida3-ma.md)

## 5. API Design
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/register | Register user | Public |
| POST | /api/v1/auth/login | Login, issue JWT | Public |
| POST | /api/v1/auth/refresh | Refresh access token | Refresh cookie |
| GET | /api/v1/listings | Search listings (city, type, size, dates) | Public |
| POST | /api/v1/listings | Create listing | Owner |
| GET | /api/v1/listings/:id | Get listing detail | Public |
| PUT | /api/v1/listings/:id | Update listing | Owner (self) |
| DELETE | /api/v1/listings/:id | Deactivate listing | Owner (self) / Admin |
| PATCH | /api/v1/listings/:id/approve | Approve/reject listing | Admin |
| POST | /api/v1/bookings | Create booking (triggers payment) | Renter |
| GET | /api/v1/bookings | List my bookings | Renter/Owner (own) |
| GET | /api/v1/bookings/:id | Booking detail (incl. access code once confirmed) | Owner (self) |
| GET | /api/v1/admin/bookings | All bookings | Admin |
| POST | /api/v1/files/upload-url | Get pre-signed upload URL | Owner |

## 6. Security Considerations
[Full detail in docs/security-wida3-ma.md]
- Authentication: JWT (short-lived access + httpOnly refresh cookie)
- Authorization: Role-based (OWNER / RENTER / ADMIN), resource-level ownership checks on listings/bookings
- Data protection: HTTPS enforced end-to-end; payment data never touches our DB directly (delegated to gateway, once chosen)
- Key risks: cold-start listing fraud, access-code leakage — see Security Baseline

## 7. Infrastructure
- Hosting: single VPS
- Database: PostgreSQL 16 + PostGIS extension, containerized
- CI/CD: GitHub Actions → build/test → Docker build → deploy to VPS via SSH + docker-compose pull/up
- Monitoring: container logs + basic uptime check (MVP); revisit if incidents demand more

## 8. Technical Risks
| Risk | Mitigation | Owner |
|---|---|---|
| Payment/storage providers undecided | Decide before Sprint 2 payment/file stories are estimated | Tech Lead + PM |
| JWT refresh flow complexity on frontend | Keep refresh logic in one shared API client interceptor | Frontend Dev |
| Single VPS = single point of failure | Nightly backups + documented restore runbook (DevOps doc) | DevOps |
