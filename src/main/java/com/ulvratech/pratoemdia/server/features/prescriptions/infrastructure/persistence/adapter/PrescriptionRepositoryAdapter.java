package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.mapper.PrescriptionMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class PrescriptionRepositoryAdapter implements PrescriptionRepository {
    private final PrescriptionJpaRepository repository;
    private final PrescriptionMapper mapper;

    public PrescriptionRepositoryAdapter(PrescriptionJpaRepository repository, PrescriptionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Prescription save(Prescription prescription) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toModel(prescription)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescription> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescription> findByVersionId(UUID versionId) {
        return repository.findDistinctByVersionsId(versionId).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Prescription> findByNutritionistAndPatient(UUID nutritionistId, UUID patientId) {
        return repository.findAllByNutritionistIdAndPatientIdAndStatusOrderByCreatedAtDesc(
            nutritionistId, patientId, PrescriptionStatus.ACTIVE).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Prescription> findCurrentByPatient(UUID patientId) {
        return repository.findCurrentByPatient(patientId, PrescriptionStatus.ACTIVE,
                PrescriptionVersionStatus.PUBLISHED, PageRequest.of(0, 1)).stream()
            .findFirst().map(mapper::toDomain);
    }
}
