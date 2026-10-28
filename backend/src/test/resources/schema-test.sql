-- Test Schema for H2 / Offline Testing
CREATE SCHEMA IF NOT EXISTS ipld11stars;

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
    last_login_at timestamp,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ipld11stars.teams (
    id varchar(255) PRIMARY KEY,
    name varchar(255) NOT NULL,
    short_name varchar(50),
    slug varchar(100),
    logo_url varchar(512),
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ipld11stars.seasons (
    id varchar(255) PRIMARY KEY,
    name varchar(255) NOT NULL,
    year int,
    starts_at timestamp,
    ends_at timestamp,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ipld11stars.matches (
    id varchar(255) PRIMARY KEY,
    season_id varchar(255),
    external_match_id varchar(255),
    match_number int,
    status varchar(50) NOT NULL DEFAULT 'UPCOMING',
    match_type varchar(50) NOT NULL DEFAULT 'LEAGUE',
    start_time_utc timestamp,
    venue varchar(255),
    team_home_id varchar(255) NOT NULL,
    team_away_id varchar(255) NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    starts_at timestamp,
    ends_at timestamp,
    status varchar(50) NOT NULL DEFAULT 'OPEN',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ipld11stars.contest_entries (
    id varchar(255) PRIMARY KEY,
    contest_id varchar(255) NOT NULL,
    user_id varchar(255) NOT NULL,
    entry_name varchar(255),
    total_points numeric(10,2) NOT NULL DEFAULT 0.00,
    rank int,
    status varchar(50) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ipld11stars.entry_player_selections (
    id varchar(255) PRIMARY KEY,
    contest_entry_id varchar(255) NOT NULL,
    player_id varchar(255) NOT NULL,
    is_captain boolean NOT NULL DEFAULT false,
    is_vice_captain boolean NOT NULL DEFAULT false,
    is_impact_player boolean NOT NULL DEFAULT false,
    is_starting_player boolean NOT NULL DEFAULT true,
    selected_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
