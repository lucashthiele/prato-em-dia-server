CREATE TABLE consent_record (
    id UUID PRIMARY KEY,
    user_account_id UUID NOT NULL,
    document_type VARCHAR(40) NOT NULL,
    version VARCHAR(40) NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_consent_record_user_account
        FOREIGN KEY (user_account_id)
        REFERENCES user_account(id),

    CONSTRAINT ck_consent_record_document_type
        CHECK (document_type IN ('TERMS_OF_USE', 'PRIVACY_POLICY')),

    CONSTRAINT ux_consent_record_acceptance
        UNIQUE (user_account_id, document_type, version)
);
