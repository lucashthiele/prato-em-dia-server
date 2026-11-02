package com.ulvratech.pratoemdia.server.devseed;

import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

/** Seeds user_account, both profile tables and nutritionist_patient_relationship. */
class IdentitySeeder {
    private record Link(SeedAccount nutritionist, SeedAccount patient, String status, String emailStatus) {
    }

    private final JdbcTemplate jdbc;
    private final PasswordHasher passwordHasher;

    IdentitySeeder(JdbcTemplate jdbc, PasswordHasher passwordHasher) {
        this.jdbc = jdbc;
        this.passwordHasher = passwordHasher;
    }

    void seed(String password) {
        var hash = passwordHasher.hash(password);
        for (var account : SeedAccount.ALL) {
            jdbc.update(
                    """
                            INSERT INTO user_account (id, email, password_hash, role, status, password_change_required)
                            VALUES (?, ?, ?, CAST(? AS user_role), CAST(? AS user_account_status), ?)
                            ON CONFLICT DO NOTHING
                            """, account.accountId(), account.email(), hash, account.role(),
                    account.active() ? "ACTIVE" : "INACTIVE", account.passwordChangeRequired()
            );
            if (account.isNutritionist()) {
                jdbc.update(
                        """
                                INSERT INTO nutritionist_profile (id, user_account_id, full_name, registration_number, phone, bio)
                                VALUES (?, ?, ?, ?, ?, ?)
                                ON CONFLICT DO NOTHING
                                """,
                        account.profileId(), account.accountId(), account.fullName(),
                        "CRN-3 " + (10000 + Math.abs(account.key().hashCode()) % 9000), "+55 11 90000-0000",
                        "Nutricionista de desenvolvimento."
                );
            } else if (account.isPatient()) {
                jdbc.update(
                        """
                                INSERT INTO patient_profile (id, user_account_id, full_name, phone, birth_date)
                                VALUES (?, ?, ?, ?, ?)
                                ON CONFLICT DO NOTHING
                                """, account.profileId(), account.accountId(), account.fullName(), "+55 11 98888-0000",
                        LocalDate.of(1990, 1, 1).plusDays(Math.abs(account.key().hashCode()) % 3000)
                );
            }
        }
        seedRelationships();
        seedBulkRelationships();
    }

    /**
     * Links the bulk patients to Ana. The email status cycles through SENT, PENDING and FAILED, and created_at is
     * scattered over distinct days (in an order unrelated to the name) so sorting visibly changes the list.
     */
    private void seedBulkRelationships() {
        var emailStatuses = java.util.List.of("SENT", "PENDING", "FAILED");
        var patients = SeedAccount.BULK_PATIENTS;
        for (int index = 0; index < patients.size(); index++) {
            var patient = patients.get(index);
            var emailStatus = emailStatuses.get(index % emailStatuses.size());
            int daysAgo = (index * 17) % patients.size() + 1;
            jdbc.update(
                    """
                            INSERT INTO nutritionist_patient_relationship (
                                id, nutritionist_id, patient_id, status, email_delivery_status, email_attempt_count,
                                email_last_attempt_at, created_at, updated_at
                            )
                            VALUES (?, ?, ?, 'ACTIVE', CAST(? AS relationship_email_status), ?,
                                    now() - make_interval(days => ?), now() - make_interval(days => ?),
                                    now() - make_interval(days => ?))
                            ON CONFLICT DO NOTHING
                            """,
                    SeedIds.of("relationship:" + patient.key()), SeedAccount.ANA.profileId(), patient.profileId(),
                    emailStatus, emailStatus.equals("PENDING") ? 0 : 1, daysAgo, daysAgo, daysAgo
            );
        }
    }

    private void seedRelationships() {
        var links = java.util.List.of(
                new Link(SeedAccount.ANA, SeedAccount.CARLA, "ACTIVE", "SENT"),
                new Link(SeedAccount.ANA, SeedAccount.DIEGO, "ACTIVE", "SENT"),
                new Link(SeedAccount.BRUNO, SeedAccount.ELISA, "ACTIVE", "SENT"),
                new Link(SeedAccount.ANA, SeedAccount.FABIO, "ACTIVE", "PENDING"),
                new Link(SeedAccount.BRUNO, SeedAccount.GABI, "ENDED", "SENT")
        );
        for (var link : links) {
            boolean ended = link.status().equals("ENDED");
            jdbc.update(
                    """
                            INSERT INTO nutritionist_patient_relationship (
                                id, nutritionist_id, patient_id, status, ended_at, ended_by_user_account_id,
                                email_delivery_status, email_attempt_count, email_last_attempt_at
                            )
                            VALUES (?, ?, ?, CAST(? AS relationship_status), CASE WHEN ? THEN now() - interval '30 days' END, ?,
                                    CAST(? AS relationship_email_status), ?, now() - interval '60 days')
                            ON CONFLICT DO NOTHING
                            """,
                    SeedIds.of("relationship:" + link.patient().key()), link.nutritionist().profileId(),
                    link.patient().profileId(), link.status(), ended,
                    ended ? link.nutritionist().accountId() : null, link.emailStatus(),
                    link.emailStatus().equals("PENDING") ? 0 : 1
            );
        }
    }
}
