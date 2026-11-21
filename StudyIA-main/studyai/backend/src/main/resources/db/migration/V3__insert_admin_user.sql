-- Mot de passe : Admin@1234 (hashé avec BCrypt strength 12)
INSERT INTO users (first_name, last_name, email, password_hash, role, status)
VALUES (
    'Admin',
    'StudyAI',
    'admin@studyai.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewFX2jnQBhVoAB5K',
    'ADMIN',
    'ACTIF'
);
