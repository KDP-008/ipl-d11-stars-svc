-- PostgreSQL DDL for IPL fantasy app schema

-- Enable pgcrypto for UUID generation if needed
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS ipld11stars;

-- Users table
CREATE TABLE IF NOT EXISTS ipld11stars.users (
    id varchar(255) PRIMARY KEY,
    user_id varchar(255) NOT NULL,
    first_name varchar(255) NOT NULL,
    last_name varchar(255),
    mobile varchar(20),
    email varchar(255),
    password_hash varchar(255),
    password_salt varchar(255),
    role varchar(50) NOT NULL DEFAULT 'PLAYER',
    status varchar(50) NOT NULL DEFAULT 'ACTIVE',
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_user_id ON ipld11stars.users(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email ON ipld11stars.users(email) WHERE email IS NOT NULL AND email != '';

-- Teams table
CREATE TABLE IF NOT EXISTS ipld11stars.teams (
    id varchar(255) PRIMARY KEY,
    name varchar(255) NOT NULL,
    short_name varchar(50),
    slug varchar(100),
    logo_url varchar(512),
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_teams_slug ON ipld11stars.teams(slug);

-- Seasons table
CREATE TABLE IF NOT EXISTS ipld11stars.seasons (
    id varchar(255) PRIMARY KEY,
    name varchar(255) NOT NULL,
    year int,
    starts_at timestamptz,
    ends_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Matches table
CREATE TABLE IF NOT EXISTS ipld11stars.matches (
    id varchar(255) PRIMARY KEY,
    season_id varchar(255),
    external_match_id varchar(255),
    match_number int,
    status varchar(50) NOT NULL DEFAULT 'UPCOMING',
    match_type varchar(50) NOT NULL DEFAULT 'LEAGUE',
    start_time_utc timestamptz,
    venue varchar(255),
    team_home_id varchar(255) NOT NULL,
    team_away_id varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_matches_start_time ON ipld11stars.matches(start_time_utc);
CREATE INDEX IF NOT EXISTS idx_matches_season_id ON ipld11stars.matches(season_id);

-- Players table
CREATE TABLE IF NOT EXISTS ipld11stars.players (
    id varchar(255) PRIMARY KEY,
    team_id varchar(255),
    full_name varchar(255) NOT NULL,
    role varchar(50),
    credits numeric(4,1) DEFAULT 8.0,
    nationality varchar(100),
    batting_style varchar(100),
    bowling_style varchar(100),
    image_url varchar(512),
    is_active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

-- Match squad table
CREATE TABLE IF NOT EXISTS ipld11stars.match_squad (
    id varchar(255) PRIMARY KEY,
    match_id varchar(255) NOT NULL,
    player_id varchar(255) NOT NULL,
    team_id varchar(255),
    is_starting_xi boolean NOT NULL DEFAULT false,
    is_impact_player boolean NOT NULL DEFAULT false,
    batting_order int,
    bowling_order int,
    created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_match_squad_match_player ON ipld11stars.match_squad(match_id, player_id);

-- Contests table
CREATE TABLE IF NOT EXISTS ipld11stars.contests (
    id varchar(255) PRIMARY KEY,
    name varchar(255) NOT NULL,
    match_id varchar(255),
    season_id varchar(255),
    created_by_user_id varchar(255) NOT NULL,
    contest_type varchar(50) NOT NULL DEFAULT 'FREE',
    visibility varchar(50) NOT NULL DEFAULT 'PUBLIC',
    invite_code varchar(100),
    max_entries int NOT NULL DEFAULT 100,
    current_entries int NOT NULL DEFAULT 0,
    entry_fee numeric(10,2) NOT NULL DEFAULT 0.00,
    prize_pool numeric(12,2) NOT NULL DEFAULT 0.00,
    max_winners int NOT NULL DEFAULT 1,
    starts_at timestamptz,
    ends_at timestamptz,
    status varchar(50) NOT NULL DEFAULT 'OPEN',
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_contests_invite_code ON ipld11stars.contests(invite_code);
CREATE INDEX IF NOT EXISTS idx_contests_match_id ON ipld11stars.contests(match_id);

-- Contest entries table
CREATE TABLE IF NOT EXISTS ipld11stars.contest_entries (
    id varchar(255) PRIMARY KEY,
    contest_id varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    entry_name varchar(255),
    total_points numeric(10,2) NOT NULL DEFAULT 0.00,
    rank int,
    status varchar(50) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_contest_entries_contest_user ON ipld11stars.contest_entries(contest_id, user_id);
CREATE INDEX IF NOT EXISTS idx_contest_entries_user_id ON ipld11stars.contest_entries(user_id);

-- Entry player selections table
CREATE TABLE IF NOT EXISTS ipld11stars.entry_player_selections (
    id varchar(255) PRIMARY KEY,
    contest_entry_id varchar(255) NOT NULL,
    player_id varchar(255) NOT NULL,
    is_captain boolean NOT NULL DEFAULT false,
    is_vice_captain boolean NOT NULL DEFAULT false,
    is_impact_player boolean NOT NULL DEFAULT false,
    is_starting_player boolean NOT NULL DEFAULT true,
    selected_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_entry_selections_entry_player ON ipld11stars.entry_player_selections(contest_entry_id, player_id);

-- Player match stats table
CREATE TABLE IF NOT EXISTS ipld11stars.player_match_stats (
    id varchar(255) PRIMARY KEY,
    match_id varchar(255) NOT NULL,
    player_id varchar(255) NOT NULL,
    runs_scored int NOT NULL DEFAULT 0,
    balls_faced int NOT NULL DEFAULT 0,
    fours int NOT NULL DEFAULT 0,
    sixes int NOT NULL DEFAULT 0,
    wickets int NOT NULL DEFAULT 0,
    overs_bowled numeric(4,1) NOT NULL DEFAULT 0.0,
    maidens int NOT NULL DEFAULT 0,
    runs_conceded int NOT NULL DEFAULT 0,
    dot_balls int NOT NULL DEFAULT 0,
    lbw_bowled int NOT NULL DEFAULT 0,
    catches int NOT NULL DEFAULT 0,
    stumpings int NOT NULL DEFAULT 0,
    run_outs int NOT NULL DEFAULT 0,
    strike_rate numeric(6,2),
    economy_rate numeric(6,2),
    points numeric(10,2) NOT NULL DEFAULT 0.00,
    created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_player_match_stats_match_player ON ipld11stars.player_match_stats(match_id, player_id);

-- ---------------------------------------------------------------------------
-- Scoring, contest entries, wallet, payments and notifications
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ipld11stars.match_deliveries (
    id varchar(255) PRIMARY KEY,
    match_id varchar(255) NOT NULL,
    innings int NOT NULL,
    sequence_no int NOT NULL,
    over_number int NOT NULL,
    ball_number int NOT NULL,
    batting_team_id varchar(255) NOT NULL,
    bowling_team_id varchar(255) NOT NULL,
    striker_id varchar(255) NOT NULL,
    non_striker_id varchar(255) NOT NULL,
    bowler_id varchar(255) NOT NULL,
    runs_batter int NOT NULL DEFAULT 0,
    extras_type varchar(20) NOT NULL DEFAULT 'NONE',
    extras_runs int NOT NULL DEFAULT 0,
    is_legal boolean NOT NULL DEFAULT true,
    is_free_hit boolean NOT NULL DEFAULT false,
    is_wicket boolean NOT NULL DEFAULT false,
    dismissal_type varchar(30),
    dismissed_player_id varchar(255),
    fielder_id varchar(255),
    created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_match_deliveries_match_innings_seq ON ipld11stars.match_deliveries(match_id, innings, sequence_no);

CREATE TABLE IF NOT EXISTS ipld11stars.contest_prizes (
    id varchar(255) PRIMARY KEY,
    contest_id varchar(255) NOT NULL,
    rank_from int NOT NULL,
    rank_to int NOT NULL,
    amount numeric(12,2) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_contest_prizes_contest_id ON ipld11stars.contest_prizes(contest_id);

CREATE TABLE IF NOT EXISTS ipld11stars.wallets (
    user_id varchar(255) PRIMARY KEY,
    balance numeric(12,2) NOT NULL DEFAULT 0.00,
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ipld11stars.wallet_transactions (
    id varchar(255) PRIMARY KEY,
    user_id varchar(255) NOT NULL,
    txn_type varchar(30) NOT NULL,
    amount numeric(12,2) NOT NULL,
    balance_after numeric(12,2) NOT NULL,
    reference_type varchar(30),
    reference_id varchar(255),
    idempotency_key varchar(255) NOT NULL,
    description varchar(500),
    created_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_transactions_idempotency_key ON ipld11stars.wallet_transactions(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_id ON ipld11stars.wallet_transactions(user_id);

CREATE TABLE IF NOT EXISTS ipld11stars.payment_orders (
    id varchar(255) PRIMARY KEY,
    user_id varchar(255) NOT NULL,
    direction varchar(20) NOT NULL,
    amount numeric(12,2) NOT NULL,
    currency varchar(3) NOT NULL DEFAULT 'INR',
    status varchar(30) NOT NULL,
    gateway_order_id varchar(255),
    gateway_reference_id varchar(255),
    failure_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT NOW(),
    updated_at timestamptz NOT NULL DEFAULT NOW()
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_orders_gateway_order_id ON ipld11stars.payment_orders(gateway_order_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_user_id ON ipld11stars.payment_orders(user_id);

CREATE TABLE IF NOT EXISTS ipld11stars.payment_webhook_events (
    event_id varchar(255) PRIMARY KEY,
    event_type varchar(100) NOT NULL,
    gateway_order_id varchar(255),
    outcome varchar(50) NOT NULL,
    payload text,
    received_at timestamptz NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ipld11stars.email_notifications (
    id varchar(255) PRIMARY KEY,
    user_id varchar(255) NOT NULL,
    recipient varchar(255),
    template varchar(60) NOT NULL,
    subject varchar(255) NOT NULL,
    body text NOT NULL,
    status varchar(20) NOT NULL,
    error varchar(500),
    created_at timestamptz NOT NULL DEFAULT NOW(),
    sent_at timestamptz
);
CREATE INDEX IF NOT EXISTS idx_email_notifications_user_id ON ipld11stars.email_notifications(user_id);
-- Columns added to existing tables (idempotent for databases created before these features)
ALTER TABLE ipld11stars.contests ADD COLUMN IF NOT EXISTS min_entries int NOT NULL DEFAULT 2;
ALTER TABLE ipld11stars.contests ADD COLUMN IF NOT EXISTS locked_at timestamptz;
ALTER TABLE ipld11stars.contests ADD COLUMN IF NOT EXISTS settled_at timestamptz;
ALTER TABLE ipld11stars.contest_entries ADD COLUMN IF NOT EXISTS entry_fee_paid numeric(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE ipld11stars.contest_entries ADD COLUMN IF NOT EXISTS prize_amount numeric(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE ipld11stars.player_match_stats ADD COLUMN IF NOT EXISTS balls_bowled int NOT NULL DEFAULT 0;
ALTER TABLE ipld11stars.player_match_stats ADD COLUMN IF NOT EXISTS is_dismissed boolean NOT NULL DEFAULT false;
ALTER TABLE ipld11stars.player_match_stats ADD COLUMN IF NOT EXISTS updated_at timestamptz NOT NULL DEFAULT NOW();
