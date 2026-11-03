CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_account_id UUID NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_account_id UUID,
    details VARCHAR(500),

    CONSTRAINT fk_audit_event_actor
        FOREIGN KEY (actor_account_id)
        REFERENCES user_account(id),

    CONSTRAINT fk_audit_event_target
        FOREIGN KEY (target_account_id)
        REFERENCES user_account(id),

    CONSTRAINT ck_audit_event_action
        CHECK (action IN ('NUTRITIONIST_CREATED', 'ACCESS_EMAIL_RESENT', 'USER_STATUS_CHANGED'))
);

CREATE INDEX ix_audit_event_occurred_at ON audit_event(occurred_at DESC);
CREATE INDEX ix_audit_event_actor ON audit_event(actor_account_id, occurred_at DESC);
CREATE INDEX ix_audit_event_target ON audit_event(target_account_id, occurred_at DESC);
