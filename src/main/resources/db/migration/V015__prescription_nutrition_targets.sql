CREATE TABLE prescription_nutrition_target (
    id UUID PRIMARY KEY,
    prescription_version_id UUID NOT NULL REFERENCES prescription_version(id) ON DELETE CASCADE,
    nutrient_code VARCHAR(64) NOT NULL,
    amount NUMERIC(16,4) NOT NULL CHECK (amount >= 0),
    unit VARCHAR(16) NOT NULL,
    CONSTRAINT ux_prescription_nutrition_target_code UNIQUE (prescription_version_id, nutrient_code)
);

CREATE FUNCTION protect_published_nutrition_target() RETURNS trigger AS $$
DECLARE
    target_version UUID;
    version_status prescription_version_status;
BEGIN
    target_version := CASE WHEN TG_OP = 'DELETE' THEN OLD.prescription_version_id ELSE NEW.prescription_version_id END;
    SELECT status INTO version_status FROM prescription_version WHERE id = target_version;
    IF version_status <> 'DRAFT' THEN
        RAISE EXCEPTION 'published nutrition targets are immutable' USING ERRCODE = '23514';
    END IF;
    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_protect_published_nutrition_target
    BEFORE INSERT OR UPDATE OR DELETE ON prescription_nutrition_target
    FOR EACH ROW EXECUTE FUNCTION protect_published_nutrition_target();
