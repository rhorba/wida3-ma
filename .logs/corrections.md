# CORRECTIONS — Wida3.ma



## 2026-07-21 — Resolved Sprint 1 open items
- Payment: mock gateway (in-process MockPaymentServiceImpl) instead of a real provider. See Architecture ADR-5.
- File storage: local disk via Docker volume instead of S3-compatible object storage. See Architecture ADR-6.
- Hosting: localhost via docker-compose stands in for a VPS; staging/production environments and remote deploy are deferred until a real host is provisioned.
- Docs updated: system-design, architecture, database, security, devops, stories, README, .env.example.
- Added Epic 5 (Launch Readiness) to stories doc to track replacing mock payment / local storage before real launch.
