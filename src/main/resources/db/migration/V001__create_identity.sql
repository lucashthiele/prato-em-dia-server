-- V001__create_identity.sql
-- Prato em Dia
-- PostgreSQL / Flyway

CREATE TYPE user_role AS ENUM (
    'PATIENT',
    'NUTRITIONIST',
    'ADMIN'
    );

CREATE TYPE user_account_status AS ENUM (
    'ACTIVE',
    'INACTIVE'
    );

CREATE TABLE user_account (
      id UUID PRIMARY KEY,
      email VARCHAR(320) NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
      role user_role NOT NULL,
      status user_account_status NOT NULL DEFAULT 'ACTIVE',
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_user_account_email_normalized
    ON user_account (LOWER(email));

CREATE INDEX ix_user_account_role
    ON user_account (role);

CREATE INDEX ix_user_account_status
    ON user_account (status);