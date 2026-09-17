package com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.importer.infrastructure.persistence.model.ImportRunModel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRunJpaRepository extends JpaRepository<ImportRunModel, UUID> {}
