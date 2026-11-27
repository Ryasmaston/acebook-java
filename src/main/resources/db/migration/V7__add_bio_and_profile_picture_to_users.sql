ALTER TABLE users ADD COLUMN bio VARCHAR(200);
ALTER TABLE users ADD COLUMN profile_picture_url TEXT;

UPDATE users SET bio = E'London 📍\ncoffee, football and long weekends\nGooner for life ⚽', profile_picture_url = 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&h=300&fit=crop&crop=faces' WHERE username = 'ryan@example.com';
UPDATE users SET bio = E'desk setup enthusiast\nplant parent to 12 (and counting)\nprobably rearranging something', profile_picture_url = 'https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=300&h=300&fit=crop&crop=faces' WHERE username = 'alex@example.com';
UPDATE users SET bio = E'🥾 out on the trails most weekends\n🐶 dog dad to Biscuit\nchasing sunsets', profile_picture_url = 'https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?w=300&h=300&fit=crop&crop=faces' WHERE username = 'sam@example.com';
UPDATE users SET bio = E'🇬🇧🇮🇹\nliving out of a suitcase ✈️\nhunting for the best gelato in every city 🍦', profile_picture_url = 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=300&h=300&fit=crop&crop=faces' WHERE username = 'jordan@example.com';