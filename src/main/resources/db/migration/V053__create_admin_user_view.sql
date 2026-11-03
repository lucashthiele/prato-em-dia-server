-- Read model of the admin user screens: account, profile name and the state of the access email.
-- Nutritionists read it from the email outbox; patients from the relationship column.
CREATE VIEW admin_user_view AS
SELECT
    u.id,
    u.email,
    u.role::text AS role,
    u.status::text AS status,
    u.password_change_required,
    u.created_at,
    COALESCE(np.full_name, pp.full_name) AS full_name,
    np.registration_number,
    CASE u.role
        WHEN 'NUTRITIONIST' THEN eo.status
        WHEN 'PATIENT' THEN rel.status
    END AS access_email_status,
    CASE u.role
        WHEN 'NUTRITIONIST' THEN eo.attempts
        WHEN 'PATIENT' THEN rel.attempts
    END AS access_email_attempts,
    CASE u.role
        WHEN 'NUTRITIONIST' THEN eo.last_error
    END AS access_email_error
FROM user_account u
LEFT JOIN nutritionist_profile np ON np.user_account_id = u.id
LEFT JOIN patient_profile pp ON pp.user_account_id = u.id
LEFT JOIN LATERAL (
    SELECT o.status, o.attempts, o.last_error
    FROM email_outbox o
    WHERE o.account_id = u.id AND o.type = 'ACCESS'
    ORDER BY o.created_at DESC
    LIMIT 1
) eo ON TRUE
LEFT JOIN LATERAL (
    SELECT r.email_delivery_status::text AS status, r.email_attempt_count AS attempts
    FROM nutritionist_patient_relationship r
    WHERE r.patient_id = pp.id
    ORDER BY r.created_at DESC
    LIMIT 1
) rel ON TRUE;
