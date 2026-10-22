ALTER TABLE prescription_version
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- The immutability trigger blocks updates on finalized versions; suspend it only for this backfill.
ALTER TABLE prescription_version DISABLE TRIGGER trg_protect_published_prescription_version;

UPDATE prescription_version SET updated_at = created_at;

ALTER TABLE prescription_version ENABLE TRIGGER trg_protect_published_prescription_version;

CREATE INDEX ix_prescription_version_draft_updated
    ON prescription_version(updated_at DESC) WHERE status = 'DRAFT';
