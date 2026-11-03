package com.ulvratech.pratoemdia.server.features.consent.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.consent.domain.entity.ConsentRecord;
import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import com.ulvratech.pratoemdia.server.features.consent.domain.port.out.ConsentRecordRepository;
import com.ulvratech.pratoemdia.server.features.consent.infrastructure.persistence.mapper.ConsentRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ConsentRecordRepositoryAdapter implements ConsentRecordRepository {
    private final ConsentRecordJpaRepository repository;
    private final ConsentRecordMapper mapper;

    public ConsentRecordRepositoryAdapter(ConsentRecordJpaRepository repository, ConsentRecordMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ConsentRecord> find(UUID userAccountId, ConsentDocumentType documentType, String version) {
        return repository.findByUserAccountIdAndDocumentTypeAndVersion(userAccountId, documentType, version)
                .map(mapper::toEntity);
    }

    @Override
    public ConsentRecord save(ConsentRecord record) {
        return mapper.toEntity(repository.saveAndFlush(mapper.toModel(record)));
    }
}
