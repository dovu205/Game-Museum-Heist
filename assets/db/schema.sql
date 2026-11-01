CREATE TABLE metadata (
 key TEXT PRIMARY KEY NOT NULL,
 value TEXT NOT NULL
);
CREATE TABLE users (
 name TEXT PRIMARY KEY NOT NULL CHECK(length(name)>0),
 salt BLOB NOT NULL CHECK(length(salt)=16),
 password_hash BLOB NOT NULL CHECK(length(password_hash)=32),
 points INTEGER NOT NULL DEFAULT 0 CHECK(points>=0),
 wins INTEGER NOT NULL DEFAULT 0 CHECK(wins>=0),
 losses INTEGER NOT NULL DEFAULT 0 CHECK(losses>=0),
 played INTEGER NOT NULL DEFAULT 0 CHECK(played>=0)
);
CREATE TABLE matches (
 id TEXT PRIMARY KEY NOT NULL,
 rounds INTEGER NOT NULL CHECK(rounds>=0),
 winner TEXT REFERENCES users(name),
 started_at INTEGER NOT NULL,
 ended_at INTEGER NOT NULL,
 reason TEXT NOT NULL
);
CREATE TABLE match_players (
 match_id TEXT NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
 user_name TEXT NOT NULL REFERENCES users(name),
 seat INTEGER NOT NULL CHECK(seat IN (0,1)),
 score INTEGER NOT NULL CHECK(score>=0),
 PRIMARY KEY(match_id,user_name),
 UNIQUE(match_id,seat)
);
CREATE INDEX idx_match_players_user ON match_players(user_name,match_id);
CREATE INDEX idx_matches_ended ON matches(ended_at DESC,id DESC);
CREATE INDEX idx_users_ranking ON users(points DESC,wins DESC);
PRAGMA user_version=1;
