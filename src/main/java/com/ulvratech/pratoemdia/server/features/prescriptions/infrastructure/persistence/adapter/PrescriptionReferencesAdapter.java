package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class PrescriptionReferencesAdapter implements PrescriptionReferences {
  private final JdbcClient jdbc;

  public PrescriptionReferencesAdapter(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<UUID> nutritionistProfileId(UUID accountId) {
    return jdbc.sql("select id from nutritionist_profile where user_account_id=:accountId")
        .param("accountId", accountId)
        .query(UUID.class)
        .optional();
  }

  @Override
  public Optional<UUID> patientProfileId(UUID accountId) {
    return jdbc.sql("select id from patient_profile where user_account_id=:accountId")
        .param("accountId", accountId)
        .query(UUID.class)
        .optional();
  }

  @Override
  public boolean hasActiveRelationship(UUID nutritionistId, UUID patientId) {
    return count(
            "select count(*) from nutritionist_patient_relationship "
                + "where nutritionist_id=:first and patient_id=:second and status='ACTIVE'",
            nutritionistId,
            patientId)
        > 0;
  }

  @Override
  public boolean isActiveFood(UUID foodId) {
    return jdbc.sql("select count(*) from food where id=:id and active=true")
            .param("id", foodId)
            .query(Integer.class)
            .single()
        > 0;
  }

  @Override
  public boolean portionBelongsToFood(UUID portionId, UUID foodId) {
    return count(
            "select count(*) from food_portion "
                + "where id=:first and food_id=:second and active=true",
            portionId,
            foodId)
        > 0;
  }

  private int count(String sql, UUID first, UUID second) {
    return jdbc.sql(sql)
        .param("first", first)
        .param("second", second)
        .query(Integer.class)
        .single();
  }
}
