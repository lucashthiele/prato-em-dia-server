package com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.model.ImportRunModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ImportRunJpaRepository extends JpaRepository<ImportRunModel, UUID> {
}
