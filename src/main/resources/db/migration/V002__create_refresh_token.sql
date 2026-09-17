CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    user_account_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    replaced_by_token_id UUID,

    CONSTRAINT fk_refresh_token_user_account
        FOREIGN KEY (user_account_id)
        REFERENCES user_account(id),

    CONSTRAINT fk_refresh_token_replacement
        FOREIGN KEY (replaced_by_token_id)
        REFERENCES refresh_token(id)
);

CREATE UNIQUE INDEX ux_refresh_token_token_hash
    ON refresh_token(token_hash);

CREATE INDEX ix_refresh_token_user_account_id
    ON refresh_token(user_account_id);

CREATE INDEX ix_refresh_token_expires_at
    ON refresh_token(expires_at);
