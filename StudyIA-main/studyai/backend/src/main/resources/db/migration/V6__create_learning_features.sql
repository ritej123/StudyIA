ALTER TABLE course_files
    ADD COLUMN storage_key VARCHAR(255),
    ADD COLUMN extracted_text TEXT;

CREATE TABLE learning_resources (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    kind        VARCHAR(20) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_learning_resources_owner_id ON learning_resources(owner_id);
CREATE INDEX idx_learning_resources_course_id ON learning_resources(course_id);
CREATE INDEX idx_learning_resources_kind ON learning_resources(kind);

CREATE TABLE study_messages (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id   BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    role        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_study_messages_course_id ON study_messages(course_id, id);
CREATE INDEX idx_study_messages_owner_id ON study_messages(owner_id);

CREATE TABLE study_attempts (
    id              BIGSERIAL PRIMARY KEY,
    owner_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resource_id     BIGINT NOT NULL REFERENCES learning_resources(id) ON DELETE CASCADE,
    score           INTEGER NOT NULL,
    total           INTEGER NOT NULL,
    answers         TEXT NOT NULL,
    completed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_study_attempts_owner_id ON study_attempts(owner_id);
CREATE INDEX idx_study_attempts_resource_id ON study_attempts(resource_id);
