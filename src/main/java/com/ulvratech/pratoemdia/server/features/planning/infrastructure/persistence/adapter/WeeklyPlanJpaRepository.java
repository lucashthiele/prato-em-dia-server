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
    /** Planos ativos que ainda têm refeição ativa; os demais viraram histórico. */
    @Query("""
            select p from WeeklyPlanModel p
            where p.patientId = :patientId and p.active = true
              and exists (select 1 from PlannedMealModel m where m.plan = p and m.supersededAt is null)
            order by p.periodStart desc
            """)
    List<WeeklyPlanModel> findAllWithActiveMeals(UUID patientId);

    @Query("""
            select distinct p from WeeklyPlanModel p join p.meals m
            where p.patientId = :patientId and p.active = true
              and m.supersededAt is null and m.date between :from and :to
            """)
    List<WeeklyPlanModel> findWithActiveMealsBetween(UUID patientId, LocalDate from, LocalDate to);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PlannedMealModel m set m.supersededAt = :now where m.plan.id = :planId and m.supersededAt is null")
    void supersedeMeals(UUID planId, Instant now);

    List<WeeklyPlanModel> findAllByPatientIdOrderByPeriodStartAsc(UUID patientId);

    Optional<WeeklyPlanModel> findFirstByPatientIdAndPeriodStartLessThanEqualAndPeriodEndGreaterThanEqualOrderByPeriodStartDesc(
            UUID patientId, LocalDate start, LocalDate end
    );

    Optional<WeeklyPlanModel> findDistinctByMealsId(UUID mealId);

    Optional<WeeklyPlanModel> findByIdAndActiveTrue(UUID id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from WeeklyPlanModel p where p.id = :planId and p.active = true")
    Optional<WeeklyPlanModel> lockActive(@org.springframework.data.repository.query.Param("planId") UUID planId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update WeeklyPlanModel p set p.active = false where p.id = :planId")
    void deactivate(UUID planId);

    List<WeeklyPlanModel> findAllByStatusAndActiveTrueAndConfirmationDeadlineLessThanEqual(
            WeeklyPlanStatus status, Instant now
    );
}
