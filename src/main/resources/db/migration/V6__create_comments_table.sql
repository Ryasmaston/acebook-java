CREATE TABLE comments (
                       id bigserial PRIMARY KEY,
                       content text NOT NULL,
                       user_id bigint REFERENCES users(id) ON DELETE SET NULL,
                       post_id bigint REFERENCES posts(id) ON DELETE SET NULL
);