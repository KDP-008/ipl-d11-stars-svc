# IPL D11 Stars

A fantasy cricket backend built with Java 21, Spring Boot 3.2, and PostgreSQL.

## What is included

- Spring Boot 3.2 REST API with Spring JDBC (`JdbcTemplate`)
- PostgreSQL database integration with auto-initialized schema (`schema.sql` / `ddl.sql`)
- JWT Authentication (`/api/auth/register`, `/api/auth/login`, `/api/auth/me`)
- Match schedule synchronization service (`IplApiService`) with BCCI/IPL feed parser
- Contest management endpoints (`/api/contests`)
- Health check endpoint (`/api/health`)
- Dockerfile and `docker-compose.yml` for 1-command local development

## Getting Started Locally

### Option 1: Run with Docker Compose (Recommended)

Start both PostgreSQL and the backend API container with one command:

```bash
docker compose up --build
```

- **Backend API**: `http://localhost:8080/api`
- **Health Check**: `http://localhost:8080/api/health`
- **PostgreSQL**: `localhost:5432` (User: `d11appuser`, Password: the `POSTGRES_PASSWORD` from your `.env`, Database: `ipld11stars`)

To run in detached (background) mode:

```bash
docker compose up -d --build
```

To stop containers:

```bash
docker compose down
```

---

### Option 2: Run with Local Maven

1. Start PostgreSQL (e.g., using Docker for DB only):

```bash
docker compose up db -d
```

2. Build and run the Spring Boot backend:

```bash
cd backend
mvn spring-boot:run
```

The service will start on port `8080` with context path `/api`.

---

## Verifying Endpoints

Once running, you can test the following endpoints:

### 1. Health Check
```bash
curl http://localhost:8080/api/health
```
Response:
```json
{"status":"UP","service":"ipl-d11-stars-backend","timestamp":1727440000000}
```

### 2. View Matches
```bash
curl http://localhost:8080/api/matches
```

### 3. Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Virat","lastName":"Kohli","userId":"virat18","password":"password123"}'
```

### 4. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userId":"virat18","password":"password123"}'
```

### 5. View Contests
```bash
curl http://localhost:8080/api/contests
```
