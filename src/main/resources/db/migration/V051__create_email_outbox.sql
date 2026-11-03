CREATE TABLE email_outbox (
    id UUID PRIMARY KEY,
    type VARCHAR(40) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(160),
    inviter_name VARCHAR(160),
    account_id UUID,
    subject VARCHAR(255),
    body TEXT,
    status VARCHAR(10) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,

    CONSTRAINT fk_email_outbox_account
        FOREIGN KEY (account_id)
        REFERENCES user_account(id),

    CONSTRAINT ck_email_outbox_type
        CHECK (type IN ('ACCESS', 'PLAN_GENERATED', 'PLAN_GENERATION_REJECTED')),

    CONSTRAINT ck_email_outbox_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'DEAD')),

    -- Access emails keep only a reference: the password is generated at send time, never stored.
    CONSTRAINT ck_email_outbox_content
        CHECK (
            (type = 'ACCESS' AND account_id IS NOT NULL AND body IS NULL)
            OR (type <> 'ACCESS' AND subject IS NOT NULL AND body IS NOT NULL)
        ),

    CONSTRAINT ck_email_outbox_attempts CHECK (attempts >= 0)
);

CREATE INDEX ix_email_outbox_due
    ON email_outbox(next_attempt_at)
    WHERE status = 'PENDING';

CREATE INDEX ix_email_outbox_account
    ON email_outbox(account_id, created_at DESC)
    WHERE account_id IS NOT NULL;
