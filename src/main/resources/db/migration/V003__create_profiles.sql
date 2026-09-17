CREATE TABLE nutritionist_profile
(
    id                  UUID PRIMARY KEY,
    user_account_id     UUID         NOT NULL,
    full_name           VARCHAR(160) NOT NULL,
    registration_number VARCHAR(80),
    phone               VARCHAR(32),
    bio                 TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ux_nutritionist_profile_user_account UNIQUE (user_account_id),
    CONSTRAINT fk_nutritionist_profile_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_account (id)
);

CREATE TABLE patient_profile
(
    id              UUID PRIMARY KEY,
    user_account_id UUID         NOT NULL,
    full_name       VARCHAR(160) NOT NULL,
    phone           VARCHAR(32),
    birth_date      DATE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT ux_patient_profile_user_account UNIQUE (user_account_id),
    CONSTRAINT fk_patient_profile_user_account
        FOREIGN KEY (user_account_id) REFERENCES user_account (id)
);

CREATE OR REPLACE FUNCTION enforce_profile_account_role()
    RETURNS TRIGGER AS
$$
DECLARE
    expected_role user_role;
    actual_role   user_role;
BEGIN
    expected_role := TG_ARGV[0]::user_role;

    SELECT role
    INTO actual_role
    FROM user_account
    WHERE id = NEW.user_account_id;

    IF actual_role IS DISTINCT FROM expected_role THEN
        RAISE EXCEPTION 'profile account role must be %', expected_role
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_nutritionist_profile_account_role
    BEFORE INSERT OR UPDATE OF user_account_id
    ON nutritionist_profile
    FOR EACH ROW
EXECUTE FUNCTION enforce_profile_account_role('NUTRITIONIST');

CREATE TRIGGER trg_patient_profile_account_role
    BEFORE INSERT OR UPDATE OF user_account_id
    ON patient_profile
    FOR EACH ROW
EXECUTE FUNCTION enforce_profile_account_role('PATIENT');

CREATE OR REPLACE FUNCTION prevent_account_profile_role_mismatch()
    RETURNS TRIGGER AS
$$
BEGIN
    IF NEW.role <> 'NUTRITIONIST'
        AND EXISTS (SELECT 1
                    FROM nutritionist_profile
                    WHERE user_account_id = NEW.id) THEN
        RAISE EXCEPTION 'account with nutritionist profile must keep NUTRITIONIST role'
            USING ERRCODE = '23514';
    END IF;

    IF NEW.role <> 'PATIENT'
        AND EXISTS (SELECT 1
                    FROM patient_profile
                    WHERE user_account_id = NEW.id) THEN
        RAISE EXCEPTION 'account with patient profile must keep PATIENT role'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_user_account_profile_role
    BEFORE UPDATE OF role
    ON user_account
    FOR EACH ROW
EXECUTE FUNCTION prevent_account_profile_role_mismatch();
