package com.ulvratech.pratoemdia.server.features.consent.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.consent.domain.entity.ConsentRecord;
import com.ulvratech.pratoemdia.server.features.consent.infrastructure.persistence.model.ConsentRecordModel;
import org.springframework.stereotype.Component;

@Component
public class ConsentRecordMapper {
    public ConsentRecord toEntity(ConsentRecordModel model) {
        return new ConsentRecord(
                model.getId(), model.getUserAccountId(), model.getDocumentType(), model.getVersion(),
                model.getAcceptedAt()
        );
    }

    public ConsentRecordModel toModel(ConsentRecord entity) {
        return ConsentRecordModel.builder()
                .id(entity.id())
                .userAccountId(entity.userAccountId())
                .documentType(entity.documentType())
                .version(entity.version())
                .acceptedAt(entity.acceptedAt())
                .build();
    }
}
