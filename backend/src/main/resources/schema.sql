-- Schema initialization for IPL D11 Stars
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
