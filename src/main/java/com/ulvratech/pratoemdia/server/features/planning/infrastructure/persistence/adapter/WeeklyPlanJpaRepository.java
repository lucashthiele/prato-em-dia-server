package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.domain.enums.WeeklyPlanStatus;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanModel;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface WeeklyPlanJpaRepository extends JpaRepository<WeeklyPlanModel, UUID> {
    List<WeeklyPlanModel> findAllByPatientIdAndActiveTrueOrderByPeriodStartDesc(UUID patientId);

    List<WeeklyPlanModel> findAllByPatientIdOrderByPeriodStartAsc(UUID patientId);

    Optional<WeeklyPlanModel> findFirstByPatientIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
            UUID patientId, LocalDate start, LocalDate end
    );

    Optional<WeeklyPlanModel> findDistinctByMealsId(UUID mealId);

    boolean existsByPatientIdAndPeriodStartAndPeriodEndAndActiveTrue(
            UUID patientId, LocalDate start, LocalDate end
    );

    Optional<WeeklyPlanModel> findByIdAndActiveTrue(UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update WeeklyPlanModel p set p.active = false where p.id = :planId")
    void deactivate(UUID planId);

    List<WeeklyPlanModel> findAllByStatusAndActiveTrueAndConfirmationDeadlineLessThanEqual(
            WeeklyPlanStatus status, Instant now
    );
}
