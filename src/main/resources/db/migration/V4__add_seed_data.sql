INSERT INTO users (username, enabled) VALUES
                                          ('ryan@example.com', TRUE),
                                          ('alex@example.com', TRUE),
                                          ('sam@example.com', TRUE),
                                          ('jordan@example.com', TRUE),
                                          ('inactive@example.com', FALSE);

INSERT INTO posts (content, user_id, title) VALUES
                                                ('Just joined Acebook. Looking forward to seeing what everyone is up to!', (SELECT id FROM users WHERE username = 'ryan@example.com'), 'Hello Acebook'),
                                                ('Finally finished setting up my new workspace. Time to get productive.', (SELECT id FROM users WHERE username = 'alex@example.com'), 'New workspace'),
                                                ('Does anyone have recommendations for a good weekend hike?', (SELECT id FROM users WHERE username = 'sam@example.com'), 'Weekend plans'),
                                                ('Spent the afternoon learning something new. Small steps add up!', (SELECT id FROM users WHERE username = 'jordan@example.com'), 'Learning every day'),
                                                ('Who else is looking forward to the weekend?', (SELECT id FROM users WHERE username = 'ryan@example.com'), 'Weekend incoming'),
                                                ('Testing out my first post on this platform.', (SELECT id FROM users WHERE username = 'alex@example.com'), 'First post');