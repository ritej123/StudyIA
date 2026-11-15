-- V4__update_admin_user.sql
-- Met à jour le compte admin existant avec les nouvelles credentials demandées
-- Email : admin@gmail.com
-- Mot de passe : Admin@1234 (hash BCrypt 12)
UPDATE users
SET email = 'admin@gmail.com',
    password_hash = '$2b$12$xKUAbn7zV8TvmnKh9oYpSObrXmK4o9kzUczdO3hgQjBlQfl8T65ta',
    updated_at = NOW()
WHERE email = 'admin@studyai.com';
