# IPL D11 Stars Service (`ipl-d11-stars-svc`)

A fantasy cricket backend service built with Java 21, Spring Boot 3.2, Spring JDBC (`JdbcTemplate`), and PostgreSQL.

## Features

- **Authentication & Users**: JWT registration, login, and profile endpoints (`/api/auth/register`, `/api/auth/login`, `/api/auth/me`) with automatic wallet provisioning.
- **Match Sync & Squad Management**: IPL schedule synchronization (`IplApiService`), 22-player squad registration, Playing XI announcements, and player credit/role management.
- **Ball-by-Ball T20 Scoring Engine**: Legal/extra delivery tracking (`NONE`, `WIDE`, `NO_BALL`, `BYE`, `LEG_BYE`), maiden over detection, dismissal validation, and full Dream11 T20 fantasy point calculation (batting, bowling, fielding, strike-rate and economy rate bonuses/penalties).
- **Contests & Fantasy XI Validation**: Dream11 11-player team validation (100.0 credit cap, role minimums/maximums, max 7 players per real team, distinct Captain 2x and Vice-Captain 1.5x multipliers).
- **Live Leaderboard, Prize Tables & Contest Settlement**: Competition ranking (`RANK()`), deterministic prize slabs (`WINNER_TAKES_ALL`, `TOP_3`, `TOP_50_PERCENT`), scheduled auto-lock at match start (`ContestLockScheduler`), and automated settlement or cancellation/refund on match completion/abandonment.
- **Wallet Ledger & Payment Gateway Webhooks**: Double-entry-style transactional wallet credits/debits with `SELECT ... FOR UPDATE` row locking, idempotency keys, deposit orders, withdrawal payouts, and HMAC-SHA256 webhook verification (`/api/webhooks/payments`).
- **Email Notifications**: Transactional post-commit email delivery (`WELCOME`, `DEPOSIT_CONFIRMED`, `WITHDRAWAL_PROCESSED`, `CONTEST_JOINED`, `CONTEST_SETTLED`, `CONTEST_REFUNDED`) with database audit trail.

## Getting Started Locally

### Option 1: Run with Docker Compose

Copy `.env.example` to `.env` and set `POSTGRES_PASSWORD` and `JWT_SECRET`, then start PostgreSQL and the backend API container:

```bash
cp .env.example .env
docker compose up --build
```

- **Backend API**: `http://localhost:8080/api`
- **Health Check**: `http://localhost:8080/api/health`
- **PostgreSQL**: `localhost:5432` (User: `d11appuser`, Database: `ipld11stars`)

### Option 2: Run with Local Maven

```bash
cd backend
mvn spring-boot:run
```

Run tests (uses in-memory H2 in PostgreSQL compatibility mode):

```bash
cd backend
mvn test
```

## Key API Endpoints

- `GET /api/health` — Service health status
- `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` — Auth & current user
- `GET /api/matches` — List matches
- `POST /api/matches/{id}/squad`, `GET /api/matches/{id}/squad` — Set and view match squad & Playing XI
- `POST /api/matches/{id}/deliveries`, `DELETE /api/matches/{id}/deliveries/last` — Record or undo ball-by-ball deliveries
- `GET /api/matches/{id}/scorecard`, `GET /api/matches/{id}/player-stats` — Match scorecard and fantasy points breakdown
- `POST /api/matches/{id}/complete`, `POST /api/matches/{id}/abandon` — Complete or abandon a match and trigger contest lifecycle transitions
- `POST /api/contests`, `GET /api/contests`, `GET /api/contests/{id}` — Create and query contests
- `POST /api/contests/{id}/entries`, `GET /api/contests/{id}/entries/me` — Submit or view user fantasy entries
- `GET /api/contests/{id}/leaderboard`, `GET /api/contests/{id}/prizes` — Live contest leaderboard and prize breakdown
- `POST /api/contests/{id}/settle`, `POST /api/contests/{id}/cancel` — Settle or cancel a contest
- `GET /api/wallet`, `POST /api/wallet/deposits`, `POST /api/wallet/withdrawals` — Wallet balance, transactions, deposits, and withdrawals
- `POST /api/webhooks/payments` — Payment gateway webhook receiver (`X-Webhook-Signature` HMAC-SHA256)
- `GET /api/notifications` — User email notification history
