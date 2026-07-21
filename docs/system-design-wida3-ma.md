# System Design: Wida3.ma
**PRD Reference**: docs/prd-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: System Designer

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | Best-effort (localhost/dev host, MVP) | Re-evaluate once a real VPS/host is provisioned |
| Latency (p99) | < 800ms for API, < 1s for search | Single-region, no CDN needed yet |
| Throughput | ~10-50 RPS | MVP traffic (Casablanca + Rabat launch) |
| Data Volume | < 1 GB/day | Mostly listing photos + booking records |
| Retention | Bookings/payments: 7 years (accounting); logs: 30 days | Morocco accounting record norms |
| Recovery (RTO) | 4 hours | Manual restore from nightly backup acceptable at MVP scale |
| Recovery (RPO) | 24 hours | Nightly Postgres dump acceptable at MVP scale |

## 2. Component Topology
```
[Web Browser: React SPA]
        ↓ HTTPS
[Nginx reverse proxy] ←── TLS termination, static asset serving
        ↓
[Spring Boot API (single service, modular monolith)]
  ├── Auth module        → [PostgreSQL]
  ├── Listings module     → [PostgreSQL + PostGIS]
  ├── Booking module      → [PostgreSQL]
  ├── Payment module       → [MockPaymentService — in-process, no external call]
  └── File storage module → [Local disk via Docker volume]
        ↓
[Docker Compose stack — runs on localhost, standing in for a VPS]

[Observability: application logs → file/volume, local log rotation for MVP]
```

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| Payment (mock) | In-process service call, simulated success/failure | No real money movement in MVP; keeps the same `PaymentService` interface a real gateway would implement later |
| File storage (local disk) | Multipart upload → API writes to a Docker-mounted volume, served as static files via Nginx | Avoids standing up object storage before it's needed; swap for S3-compatible storage later without changing the API contract |
| Frontend ↔ Backend | REST/JSON over HTTPS | Simplest fit for a React SPA + Spring Boot API, no real-time requirement in MVP |

## 4. Scalability Strategy
- Scaling approach: vertical, running on localhost for MVP; re-evaluate once deployed to a real VPS/host
- Cache strategy: none at MVP — add Redis only if search/listing read load becomes a measured bottleneck
- Queue strategy: none at MVP — synchronous payment confirmation is acceptable at current volume; revisit with a queue (e.g. for async email/SMS notifications) if notification volume grows

## 5. System Design Decision Records

### SDR-1: Modular monolith over microservices
- **NFR Driver**: Availability + team size (single small team, MVP scope)
- **Decision**: Single Spring Boot application with clear module boundaries (auth, listings, booking, payment, files) rather than separate services
- **Alternatives**: Microservices — rejected as premature; adds deployment/ops complexity with no current scale justification
- **Re-evaluate when**: Team grows beyond ~1-2 backend devs per domain, or a module's load profile diverges sharply from the rest

### SDR-2: Docker Compose on localhost (standing in for a VPS) over managed cloud/K8s
- **NFR Driver**: Cost + MVP scale (10-50 RPS) + no hosting budget committed yet
- **Decision**: Deploy via docker-compose on localhost for now; the compose stack is written host-agnostically so it can move to a rented VPS unchanged when one is provisioned
- **Alternatives**: Managed cloud (AWS ECS/EKS) — rejected as overkill for current traffic and budget. Renting a VPS now — deferred, not rejected; revisit once ready to have a publicly reachable environment
- **Re-evaluate when**: Ready to demo/launch to real users (localhost isn't publicly reachable), or traffic outgrows a single host

### SDR-3: PostgreSQL + PostGIS as the only datastore
- **NFR Driver**: Data volume (< 1GB/day) + city/geo search requirement (FR-2)
- **Decision**: One PostgreSQL instance with the PostGIS extension for city-based search; no separate search engine
- **Alternatives**: Elasticsearch — rejected, unnecessary at MVP listing volume (tens to low hundreds of listings)
- **Re-evaluate when**: Listing count grows into the thousands and free-text/relevance search becomes a real need

### SDR-4: Mock payment, local disk storage for MVP
- **NFR Driver**: N/A — explicit user decision this session, resolving the prior TBD
- **Decision**: Payment is simulated via an in-process `MockPaymentService` (configurable success/failure, no real provider). Listing photos are stored on local disk via a Docker-mounted volume, not object storage.
- **Alternatives**: Real gateway (CMI) — deferred until real transactions are needed. S3-compatible storage — deferred until deployment moves off a single local host.
- **Re-evaluate when**: Before accepting real payments (must integrate a real gateway before launch) and before deploying to a host where local disk isn't durable/shared (must move to object storage before scaling past one host)
