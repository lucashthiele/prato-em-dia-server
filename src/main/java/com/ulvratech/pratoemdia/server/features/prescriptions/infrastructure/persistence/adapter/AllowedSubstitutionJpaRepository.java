package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.AllowedSubstitutionModel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AllowedSubstitutionJpaRepository extends JpaRepository<AllowedSubstitutionModel, UUID> {
    @Query("""
            select count(s) from AllowedSubstitutionModel s
            where s.id = :id and s.item.id = :itemId and s.foodId = :foodId
              and (s.foodPortionId is null or s.foodPortionId = :portionId)
            """)
    long countAuthorized(
            @Param("id") UUID id, @Param("itemId") UUID itemId, @Param("foodId") UUID foodId,
            @Param("portionId") UUID portionId
    );
}
