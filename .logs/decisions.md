# DECISIONS — Wida3.ma



## 2026-07-21 — Stack pivot & scope
- Stack changed from README's Next.js/Drizzle to: Backend Java 21 + Spring Boot 3, Frontend React + TypeScript, DB PostgreSQL + PostGIS, Docker/docker-compose deployment on a single VPS.
- Frontend framework: React chosen over Angular (YAGNI — user picked React).
- Payment (CMI) and file storage provider: deferred/TBD, to be decided before those features are built.
- Git remote confirmed: https://github.com/rhorba/wida3-ma (no local .git yet — will init this session).
- Sprint 1 scope: full foundation doc set, no code.

## 2026-07-21 — Sprint 1 open items resolved
- Mock payment gateway, local disk file storage, localhost-as-VPS deployment — all confirmed by user. Real gateway/object storage/VPS integration deferred to a pre-launch Epic 5 (stories 5.1, 5.2).

## 2026-07-22 — Story 1.1 approach
- User chose 🔴 COMPREHENSIVE over 🟡 BALANCED (recommended) for Story 1.1 (registration/login): scaffold + BCrypt + breach-list password check + integration tests + rate-limiting on auth endpoints + account lockout after failed attempts. Lockout/rate-limiting go beyond the story's written acceptance criteria but were explicitly requested.

## 2026-07-22 — Story 1.2 scope
- User chose "full stack now" over "backend only" for Story 1.2 (JWT refresh & logout): also scaffolding the React + TypeScript frontend project this session, rather than deferring it to a separate story/session.
