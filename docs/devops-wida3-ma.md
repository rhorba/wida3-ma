# DevOps Foundation: Wida3.ma
**Architecture**: docs/architecture-wida3-ma.md
**Security**: docs/security-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: DevOps/DevSecOps

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development AND current "production" — `docker-compose up` on localhost | Manual |
| staging | Deferred — no remote host provisioned yet | N/A |
| production | Deferred — no remote host provisioned yet | N/A |

There is currently one environment: your machine, running the full docker-compose stack. When a real VPS is provisioned later, re-introduce staging/production as separate deploy targets (the compose stack is written host-agnostically so this is a config change, not a rewrite).

## 2. CI Pipeline (GitHub Actions)
```yaml
stages:
  - lint            # ESLint (frontend), Checkstyle/Spotless (backend)
  - test            # JUnit + Vitest, fail CI if combined coverage < 80%
  - security-scan   # SAST (Semgrep), SCA (Trivy for Maven/npm + Docker images), secrets (Gitleaks)
  - build           # Docker images: backend (Spring Boot jar) + frontend (static build served by Nginx)
  # deploy stages omitted — nothing to deploy to remotely yet.
  # Re-add deploy-staging / deploy-prod once a VPS exists.
```

## 3. Infrastructure
- **Hosting**: Localhost, via `docker-compose up`. No VPS provisioned yet — this stack is host-agnostic so moving it to a rented VPS later is a deploy-target change, not a redesign.
- **Compute**: Docker containers via docker-compose (api, web, postgres, nginx)
- **Database**: Self-hosted PostgreSQL 16 + PostGIS container, with a named volume. Backup: manual `pg_dump` for now (no scheduled cron until this runs somewhere persistent/always-on)
- **Secrets**: local `.env` file (never committed) — see `.env.example` at repo root
- **Monitoring**: Container logs (`docker compose logs`) only — no external uptime checker while running on localhost (nothing public to check)

## 4. Security Scanning Gates
| Scanner | Scan Type | Fail Threshold |
|---|---|---|
| Semgrep | SAST — code vulnerabilities | Critical findings |
| Trivy | SCA — dependency CVEs + Docker image scan | Critical CVEs |
| Gitleaks | Secrets detection | Any secrets found |

## 5. Docker Setup

### docker-compose.yml (outline)
```yaml
services:
  postgres:
    image: postgis/postgis:16-3.4
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    restart: unless-stopped

  api:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      PAYMENT_MODE: MOCK
      FILE_STORAGE_PATH: /app/uploads
    volumes:
      - uploads:/app/uploads
    depends_on: [postgres]
    restart: unless-stopped

  web:
    build: ./frontend
    restart: unless-stopped

  nginx:
    image: nginx:1.27-alpine
    ports: ["8080:80"]                 # plain HTTP on localhost — no TLS cert needed until deployed to a real host
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - uploads:/usr/share/nginx/html/uploads:ro   # served as static files
    depends_on: [api, web]
    restart: unless-stopped

volumes:
  pgdata:
  uploads:
```

### backend/Dockerfile (outline)
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
USER 1000:1000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### frontend/Dockerfile (outline)
```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
```

## 6. Monitoring Baseline
| Signal | Tool | Alert Threshold |
|---|---|---|
| Logs | docker compose logs (MVP) | Manual review; revisit centralized logging post-MVP |
| Uptime | N/A — localhost only, nothing public to monitor | Add once deployed to a real host |
| Latency | Deferred — no APM at MVP | Revisit if p99 NFR (docs/system-design) is at risk |

## Open Items
- No VPS provisioned — running on localhost until one is chosen. Re-add staging/production environments and a deploy step when that happens.
- Payment and file storage are mocked/local by design for MVP (see Architecture ADR-5/ADR-6) — both must become real integrations before accepting real users/money or deploying off a single host.
