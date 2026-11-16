ALTER TABLE audit_event DROP CONSTRAINT ck_audit_event_action;

ALTER TABLE audit_event
    ADD CONSTRAINT ck_audit_event_action
        CHECK (action IN (
            'NUTRITIONIST_CREATED', 'ACCESS_EMAIL_RESENT', 'USER_STATUS_CHANGED', 'RELATIONSHIPS_ENDED'
        ));
