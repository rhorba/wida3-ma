# COMMUNICATIONS — Wida3.ma



## 2026-07-21 — UNDERSTAND phase
User request: start Sprint 1, document all foundation docs, stack = Java Spring + React + Docker.
Resolved: stack pivot confirmed, React over Angular, full doc set, VPS + docker-compose hosting, payment/storage deferred, git remote provided.

## 2026-07-30 — UNDERSTAND: CI pipeline setup
Scope: existing codebase, continuation (not new project). Stack confirmed: backend Maven/Spring Boot 3.3.4/Java 21, JaCoCo 0.8.12, Testcontainers (Postgres) for integration tests; frontend Vite/TS/oxlint, Playwright e2e only (no unit test runner configured). No .github/workflows yet, no Dockerfile yet. GitHub remote (rhorba/wida3-ma) confirmed, gh CLI authenticated. Backend requires SPRING_DATASOURCE_URL/DB_USER/DB_PASSWORD/JWT_SECRET as env vars with no defaults (per .env.example) -- CI test job will need dummy placeholder values set directly in the workflow (not real secrets, matching how local dev uses .env).
Specialist: DevOps/DevSecOps (primary), reference `references/cicd-security.md` loaded. Moving to BRAINSTORM.
