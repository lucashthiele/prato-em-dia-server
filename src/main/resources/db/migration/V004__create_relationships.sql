ALTER TABLE user_account
    ADD COLUMN password_change_required BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TYPE relationship_status AS ENUM ('ACTIVE', 'ENDED');
CREATE TYPE relationship_email_status AS ENUM ('PENDING', 'SENT', 'FAILED');

CREATE TABLE nutritionist_patient_relationship
(
    id                       UUID PRIMARY KEY,
    nutritionist_id          UUID                      NOT NULL REFERENCES nutritionist_profile (id),
    patient_id               UUID                      NOT NULL REFERENCES patient_profile (id),
    status                   relationship_status       NOT NULL DEFAULT 'ACTIVE',
    ended_at                 TIMESTAMPTZ,
    ended_by_user_account_id UUID REFERENCES user_account (id),
    email_delivery_status    relationship_email_status NOT NULL DEFAULT 'PENDING',
    email_attempt_count      INTEGER                   NOT NULL DEFAULT 0 CHECK (email_attempt_count >= 0),
    email_last_attempt_at    TIMESTAMPTZ,
    created_at               TIMESTAMPTZ               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ               NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                  BIGINT                    NOT NULL DEFAULT 0,
    CONSTRAINT ck_relationship_end_state CHECK (
        (status = 'ACTIVE' AND ended_at IS NULL AND ended_by_user_account_id IS NULL)
            OR (status = 'ENDED' AND ended_at IS NOT NULL AND ended_by_user_account_id IS NOT NULL)
        )
);

CREATE UNIQUE INDEX ux_relationship_one_active_per_patient
    ON nutritionist_patient_relationship (patient_id)
    WHERE status = 'ACTIVE';
CREATE INDEX ix_relationship_nutritionist_status
    ON nutritionist_patient_relationship (nutritionist_id, status);
CREATE INDEX ix_relationship_patient_status
    ON nutritionist_patient_relationship (patient_id, status);
