# Planify – Project & Task Management System

Full-stack Workspace → Project → Task management platform built with **Spring Boot 3 (Java 21)**, **MySQL 8**, **Redis 7**, and **React 18 (Vite)**.

## Features
- **Authentication & Security**: JWT access tokens (15 min) in memory + HttpOnly `SameSite=Strict` refresh token cookies (7 days) with automatic rotation, reuse detection, and Bucket4j login rate limiting.
- **Multi-level RBAC**: Platform (`SUPER_ADMIN`, `USER`), Workspace (`OWNER`, `ADMIN`, `MEMBER`), and Project (`PROJECT_MANAGER`, `CONTRIBUTOR`, `VIEWER`) roles enforced via `@PreAuthorize` + `PermissionService` (Redis-cached with DB fallback).
- **Task Workflows**: Server-side pagination, JPA Specification filtering (`status`, `priority`, `assigneeId`, `dueFrom`, `dueTo`), MySQL `FULLTEXT` search, sorting whitelist, soft deletes, and versioned Redis cache invalidation.
- **Audit Tracking**: Asynchronous (`@Async`) insert-only audit trail via Spring AOP `@Audited` capturing entity snapshots and actor metadata.
- **API Docs**: OpenAPI / Swagger UI at `/swagger-ui.html`.

## Quick Start (Docker Compose)
```bash
docker compose up --build
```
- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Seeded Admin Account
- Email: `admin@planify.local`
- Password: `password` *(change immediately in production)*

## Local Development

### Backend
Requires Java 21+, Maven 3.9+, MySQL 8, and Redis 7.
```bash
cd backend
mvn spring-boot:run
```
Run unit + Testcontainers integration tests (requires Docker):
```bash
cd backend
mvn test
```

### Frontend
Requires Node 20+.
```bash
cd frontend
npm install
npm run dev
```
Run frontend tests:
```bash
cd frontend
npm test
```
