# DevOps Foundation: Wida3.ma
**Architecture**: docs/architecture-wida3-ma.md
**Security**: docs/security-wida3-ma.md
**Version**: 1.0 | **Date**: 2026-07-21 | **Author**: DevOps/DevSecOps

## 1. Environment Strategy
| Environment | Purpose | Deploy Trigger |
|---|---|---|
| local | Development (docker-compose up) | Manual |
| staging | QA / Preview | Auto on PR merge to `main` |
| production | Live users | Manual tag / approved release, deployed to the single VPS |

## 2. CI Pipeline (GitHub Actions)
```yaml
stages:
  - lint            # ESLint (frontend), Checkstyle/Spotless (backend)
  - test            # JUnit + Vitest, fail CI if combined coverage < 80%
  - security-scan   # SAST (Semgrep), SCA (Trivy for Maven/npm + Docker images), secrets (Gitleaks)
  - build           # Docker images: backend (Spring Boot jar) + frontend (static build served by Nginx)
  - deploy-staging  # auto on PR merge to main
  - deploy-prod     # manual approval gate, SSH + docker-compose pull/up on the VPS
```

## 3. Infrastructure
- **Hosting**: Single VPS (provider TBD by user — e.g. OVH/DigitalOcean/Hetzner all fit a Morocco-adjacent low-latency budget setup)
- **Compute**: Docker containers via docker-compose (api, web, postgres, nginx)
- **Database**: Self-hosted PostgreSQL 16 + PostGIS container, with a named volume + nightly `pg_dump` backup cron
- **Secrets**: `.env` file on the VPS (never committed), populated from GitHub Actions secrets during deploy; see `.env.example` at repo root
- **Monitoring**: Container logs (docker logs / `docker compose logs`) for MVP; simple external uptime check (e.g. UptimeRobot) on the public URL

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
      PAYMENT_PROVIDER_KEY: ${PAYMENT_PROVIDER_KEY}   # TBD provider
      FILE_STORAGE_ENDPOINT: ${FILE_STORAGE_ENDPOINT}  # TBD provider
      FILE_STORAGE_KEY: ${FILE_STORAGE_KEY}
      FILE_STORAGE_SECRET: ${FILE_STORAGE_SECRET}
    depends_on: [postgres]
    restart: unless-stopped

  web:
    build: ./frontend
    restart: unless-stopped

  nginx:
    image: nginx:1.27-alpine
    ports: ["443:443", "80:80"]
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./certs:/etc/nginx/certs:ro
    depends_on: [api, web]
    restart: unless-stopped

volumes:
  pgdata:
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
| Uptime | External uptime checker (e.g. UptimeRobot) | Downtime > 5 min |
| Latency | Deferred — no APM at MVP | Revisit if p99 NFR (docs/system-design) is at risk |

## Open Items
- VPS provider not yet chosen by user.
- Payment gateway and object storage provider not yet chosen — env vars above are named placeholders (`PAYMENT_PROVIDER_KEY`, `FILE_STORAGE_*`) pending that decision.
