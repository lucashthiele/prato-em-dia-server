CREATE TYPE prescription_status AS ENUM ('ACTIVE', 'ARCHIVED');
CREATE TYPE prescription_version_status AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');

CREATE TABLE prescription (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient_profile(id),
    nutritionist_id UUID NOT NULL REFERENCES nutritionist_profile(id),
    status prescription_status NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_prescription_patient ON prescription(patient_id);
CREATE INDEX ix_prescription_nutritionist ON prescription(nutritionist_id);
CREATE INDEX ix_prescription_nutritionist_patient
    ON prescription(nutritionist_id, patient_id, created_at DESC);
CREATE UNIQUE INDEX ux_prescription_one_active_timeline
    ON prescription(nutritionist_id, patient_id) WHERE status = 'ACTIVE';

CREATE TABLE prescription_version (
    id UUID PRIMARY KEY,
    prescription_id UUID NOT NULL REFERENCES prescription(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL CHECK (version_number > 0),
    status prescription_version_status NOT NULL DEFAULT 'DRAFT',
    title VARCHAR(180),
    notes TEXT,
    created_by_user_id UUID NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMPTZ,
    archived_at TIMESTAMPTZ,
    CONSTRAINT ux_prescription_version_number UNIQUE(prescription_id, version_number),
    CONSTRAINT ck_prescription_version_dates CHECK (
        (status = 'DRAFT' AND published_at IS NULL AND archived_at IS NULL)
        OR (status = 'PUBLISHED' AND published_at IS NOT NULL AND archived_at IS NULL)
        OR (status = 'ARCHIVED' AND archived_at IS NOT NULL)
    )
);

CREATE INDEX ix_prescription_version_prescription_status
    ON prescription_version(prescription_id, status);
CREATE UNIQUE INDEX ux_prescription_one_published_version
    ON prescription_version(prescription_id) WHERE status = 'PUBLISHED';

CREATE TABLE prescribed_meal (
    id UUID PRIMARY KEY,
    prescription_version_id UUID NOT NULL REFERENCES prescription_version(id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    meal_order INTEGER NOT NULL CHECK (meal_order >= 0),
    suggested_time TIME,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_prescribed_meal_order UNIQUE(prescription_version_id, meal_order)
);

CREATE INDEX ix_prescribed_meal_version ON prescribed_meal(prescription_version_id);

CREATE TABLE prescribed_meal_item (
    id UUID PRIMARY KEY,
    prescribed_meal_id UUID NOT NULL REFERENCES prescribed_meal(id) ON DELETE CASCADE,
    food_id UUID NOT NULL REFERENCES food(id),
    food_portion_id UUID REFERENCES food_portion(id),
    quantity NUMERIC(12,3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(32) NOT NULL,
    item_order INTEGER NOT NULL CHECK (item_order >= 0),
    notes TEXT,
    CONSTRAINT ux_prescribed_meal_item_order UNIQUE(prescribed_meal_id, item_order)
);

CREATE INDEX ix_prescribed_meal_item_meal ON prescribed_meal_item(prescribed_meal_id);
CREATE INDEX ix_prescribed_meal_item_food ON prescribed_meal_item(food_id);

CREATE TABLE allowed_substitution (
    id UUID PRIMARY KEY,
    prescribed_meal_item_id UUID NOT NULL REFERENCES prescribed_meal_item(id) ON DELETE CASCADE,
    substitute_food_id UUID NOT NULL REFERENCES food(id),
    substitute_food_portion_id UUID REFERENCES food_portion(id),
    quantity NUMERIC(12,3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(32) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_allowed_substitution UNIQUE(
        prescribed_meal_item_id, substitute_food_id, substitute_food_portion_id
    )
);

CREATE INDEX ix_allowed_substitution_item ON allowed_substitution(prescribed_meal_item_id);

CREATE OR REPLACE FUNCTION protect_published_prescription_version()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' AND OLD.status <> 'DRAFT' THEN
        RAISE EXCEPTION 'finalized prescription versions are immutable' USING ERRCODE = '23514';
    END IF;
    IF TG_OP = 'UPDATE' AND OLD.status <> 'DRAFT' AND NOT (
        OLD.status = 'PUBLISHED' AND NEW.status = 'ARCHIVED'
        AND OLD.id = NEW.id
        AND OLD.prescription_id = NEW.prescription_id
        AND OLD.version_number = NEW.version_number
        AND OLD.title IS NOT DISTINCT FROM NEW.title
        AND OLD.notes IS NOT DISTINCT FROM NEW.notes
        AND OLD.created_by_user_id = NEW.created_by_user_id
        AND OLD.created_at = NEW.created_at
        AND OLD.published_at = NEW.published_at
        AND NEW.archived_at IS NOT NULL
    ) THEN
        RAISE EXCEPTION 'finalized prescription versions are immutable' USING ERRCODE = '23514';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_published_prescription_version
    BEFORE UPDATE OR DELETE ON prescription_version
    FOR EACH ROW EXECUTE FUNCTION protect_published_prescription_version();

CREATE OR REPLACE FUNCTION protect_published_prescription_content()
RETURNS TRIGGER AS $$
DECLARE
    immutable BOOLEAN := FALSE;
BEGIN
    IF TG_TABLE_NAME = 'prescribed_meal' THEN
        IF TG_OP <> 'INSERT' THEN
            SELECT status <> 'DRAFT' INTO immutable FROM prescription_version
            WHERE id = OLD.prescription_version_id;
        END IF;
        IF NOT immutable AND TG_OP <> 'DELETE' THEN
            SELECT status <> 'DRAFT' INTO immutable FROM prescription_version
            WHERE id = NEW.prescription_version_id;
        END IF;
    ELSIF TG_TABLE_NAME = 'prescribed_meal_item' THEN
        IF TG_OP <> 'INSERT' THEN
            SELECT pv.status <> 'DRAFT' INTO immutable
            FROM prescribed_meal pm JOIN prescription_version pv ON pv.id = pm.prescription_version_id
            WHERE pm.id = OLD.prescribed_meal_id;
        END IF;
        IF NOT immutable AND TG_OP <> 'DELETE' THEN
            SELECT pv.status <> 'DRAFT' INTO immutable
            FROM prescribed_meal pm JOIN prescription_version pv ON pv.id = pm.prescription_version_id
            WHERE pm.id = NEW.prescribed_meal_id;
        END IF;
    ELSE
        IF TG_OP <> 'INSERT' THEN
            SELECT pv.status <> 'DRAFT' INTO immutable
            FROM prescribed_meal_item pmi
            JOIN prescribed_meal pm ON pm.id = pmi.prescribed_meal_id
            JOIN prescription_version pv ON pv.id = pm.prescription_version_id
            WHERE pmi.id = OLD.prescribed_meal_item_id;
        END IF;
        IF NOT immutable AND TG_OP <> 'DELETE' THEN
            SELECT pv.status <> 'DRAFT' INTO immutable
            FROM prescribed_meal_item pmi
            JOIN prescribed_meal pm ON pm.id = pmi.prescribed_meal_id
            JOIN prescription_version pv ON pv.id = pm.prescription_version_id
            WHERE pmi.id = NEW.prescribed_meal_item_id;
        END IF;
    END IF;
    IF immutable THEN
        RAISE EXCEPTION 'published prescription content is immutable' USING ERRCODE = '23514';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_published_meal
    BEFORE INSERT OR UPDATE OR DELETE ON prescribed_meal
    FOR EACH ROW EXECUTE FUNCTION protect_published_prescription_content();
CREATE TRIGGER trg_protect_published_meal_item
    BEFORE INSERT OR UPDATE OR DELETE ON prescribed_meal_item
    FOR EACH ROW EXECUTE FUNCTION protect_published_prescription_content();
CREATE TRIGGER trg_protect_published_substitution
    BEFORE INSERT OR UPDATE OR DELETE ON allowed_substitution
    FOR EACH ROW EXECUTE FUNCTION protect_published_prescription_content();
