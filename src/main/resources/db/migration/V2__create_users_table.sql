CREATE TABLE posts (
  id bigserial PRIMARY KEY,
  content text NOT NULL,
  user_id bigint REFERENCES users(id) ON DELETE SET NULL
);