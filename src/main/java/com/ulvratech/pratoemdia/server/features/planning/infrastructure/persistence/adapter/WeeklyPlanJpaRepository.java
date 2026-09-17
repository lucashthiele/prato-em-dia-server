package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.domain.enums.WeeklyPlanStatus;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanModel;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WeeklyPlanJpaRepository extends JpaRepository<WeeklyPlanModel, UUID> {
    List<WeeklyPlanModel> findAllByPatientIdOrderByPeriodStartDesc(UUID patientId);

    boolean existsByPatientIdAndPeriodStartAndPeriodEnd(
            UUID patientId, LocalDate start, LocalDate end
    );

    List<WeeklyPlanModel> findAllByStatusAndConfirmationDeadlineLessThanEqual(
            WeeklyPlanStatus status, Instant now
    );
}
