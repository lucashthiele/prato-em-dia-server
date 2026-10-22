ALTER TABLE prescription_version
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE prescription_version SET updated_at = created_at;

CREATE INDEX ix_prescription_version_draft_updated
    ON prescription_version(updated_at DESC) WHERE status = 'DRAFT';
