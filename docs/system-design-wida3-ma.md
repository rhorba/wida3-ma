# System Design: Wida3.ma
**PRD Reference**: docs/prd-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: System Designer

## 1. Non-Functional Requirements
| Attribute | Target | Notes |
|---|---|---|
| Availability | 99% (single VPS, MVP) | Re-evaluate at multi-region if uptime SLA tightens |
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
  ├── Payment module       → [External payment gateway — TBD]
  └── File storage module → [Object storage — TBD, S3-compatible]
        ↓
[Docker Compose stack on single VPS]

[Observability: application logs → file/volume, shipped to VPS-local log rotation for MVP]
```

## 3. Integration Patterns
| Integration | Pattern | Reason |
|---|---|---|
| Payment gateway (TBD) | REST (synchronous) + webhook callback | Booking must confirm only after payment success |
| Object storage (TBD, S3-compatible) | REST (pre-signed upload URLs) | Avoids routing large file uploads through the API server |
| Frontend ↔ Backend | REST/JSON over HTTPS | Simplest fit for a React SPA + Spring Boot API, no real-time requirement in MVP |

## 4. Scalability Strategy
- Scaling approach: vertical (single VPS) for MVP; re-evaluate horizontal scaling only if sustained load approaches VPS capacity
- Cache strategy: none at MVP — add Redis only if search/listing read load becomes a measured bottleneck
- Queue strategy: none at MVP — synchronous payment confirmation is acceptable at current volume; revisit with a queue (e.g. for async email/SMS notifications) if notification volume grows

## 5. System Design Decision Records

### SDR-1: Modular monolith over microservices
- **NFR Driver**: Availability + team size (single small team, MVP scope)
- **Decision**: Single Spring Boot application with clear module boundaries (auth, listings, booking, payment, files) rather than separate services
- **Alternatives**: Microservices — rejected as premature; adds deployment/ops complexity with no current scale justification
- **Re-evaluate when**: Team grows beyond ~1-2 backend devs per domain, or a module's load profile diverges sharply from the rest

### SDR-2: Single VPS + Docker Compose over managed cloud/K8s
- **NFR Driver**: Cost + MVP scale (10-50 RPS)
- **Decision**: Deploy via docker-compose on a single VPS
- **Alternatives**: Managed cloud (AWS ECS/EKS) — rejected as overkill for current traffic and budget
- **Re-evaluate when**: Uptime SLA requirement exceeds what one VPS can offer, or traffic outgrows vertical scaling headroom

### SDR-3: PostgreSQL + PostGIS as the only datastore
- **NFR Driver**: Data volume (< 1GB/day) + city/geo search requirement (FR-2)
- **Decision**: One PostgreSQL instance with the PostGIS extension for city-based search; no separate search engine
- **Alternatives**: Elasticsearch — rejected, unnecessary at MVP listing volume (tens to low hundreds of listings)
- **Re-evaluate when**: Listing count grows into the thousands and free-text/relevance search becomes a real need

### SDR-4: Payment gateway & object storage left TBD
- **NFR Driver**: N/A — explicit deferral per user decision this session
- **Decision**: Architecture defines an integration seam (payment module, file storage module) but the concrete provider (CMI vs alternative; S3 vs MinIO vs other) is not chosen yet
- **Alternatives**: N/A
- **Re-evaluate when**: Before Sprint 2 stories touching payment or file upload begin — must be decided before those stories are estimated
