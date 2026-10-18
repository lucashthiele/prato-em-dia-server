package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionModel;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MealExecutionJpaRepository extends JpaRepository<MealExecutionModel, UUID> {
    interface NutrientRow {
        String getCode();

        BigDecimal getAmount();

        String getUnit();
    }

    Optional<MealExecutionModel> findByIdAndPatientId(UUID id, UUID patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from MealExecutionModel e where e.id = :id and e.patientId = :patientId")
    Optional<MealExecutionModel> findForUpdate(
            @Param("id") UUID id, @Param("patientId") UUID patientId
    );

    List<MealExecutionModel> findAllByPatientIdAndDateAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(
            UUID patientId, LocalDate date
    );

    @Query("""
            select distinct e.date from MealExecutionModel e
            where e.patientId = :patientId and e.plannedMealId is null and e.deletedAt is null
            order by e.date
            """)
    List<LocalDate> findFreeExecutionDates(@Param("patientId") UUID patientId);

    @Query("""
            select s.nutrientCode as code, sum(s.amount) as amount, s.unit as unit
            from MealExecutionNutrientSnapshotModel s
            where s.item.execution.patientId = :patientId and s.item.execution.date = :date
              and s.item.execution.deletedAt is null
            group by s.nutrientCode, s.unit
            """)
    List<NutrientRow> sumNutrients(@Param("patientId") UUID patientId, @Param("date") LocalDate date);
}
