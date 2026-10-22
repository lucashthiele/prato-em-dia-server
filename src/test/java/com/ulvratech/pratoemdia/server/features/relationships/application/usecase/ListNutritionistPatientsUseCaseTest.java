package com.ulvratech.pratoemdia.server.features.relationships.application.usecase;

import com.ulvratech.pratoemdia.server.features.relationships.application.dto.ListRelationshipsInput;
import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipFilter;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipSort;
import com.ulvratech.pratoemdia.server.features.relationships.domain.port.out.RelationshipRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListNutritionistPatientsUseCaseTest {
    private final RelationshipRepository repository = mock(RelationshipRepository.class);
    private final ListNutritionistPatientsUseCase useCase = new ListNutritionistPatientsUseCase(repository);
    private final UUID account = UUID.randomUUID();

    @Test
    void usesDefaultsWhenNoOptionalParameterIsInformed() {
        stub();

        useCase.execute(new ListRelationshipsInput(account, 0, 20));

        verify(repository).findActiveByNutritionistAccount(
                account, RelationshipFilter.none(), new PageRequest(0, 20)
        );
    }

    @Test
    void normalizesSearchAndMapsFilters() {
        stub();

        useCase.execute(
                new ListRelationshipsInput(
                        account, 1, 10, "  JoÃO Álvares ", RelationshipEmailStatus.FAILED, true,
                        "fullName,asc"
                )
        );

        verify(repository).findActiveByNutritionistAccount(
                account,
                new RelationshipFilter(
                        "joao alvares", RelationshipEmailStatus.FAILED, true,
                        RelationshipSort.NAME_ASC
                ),
                new PageRequest(1, 10)
        );
    }

    @Test
    void treatsBlankSearchAndFalseDraftAsAbsent() {
        stub();

        useCase.execute(
                new ListRelationshipsInput(account, 0, 20, "   ", null, false, "createdAt,asc")
        );

        verify(repository).findActiveByNutritionistAccount(
                account,
                new RelationshipFilter(null, null, false, RelationshipSort.CREATED_AT_ASC),
                new PageRequest(0, 20)
        );
    }

    @Test
    void rejectsUnsupportedSort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.execute(
                        new ListRelationshipsInput(account, 0, 20, null, null, null, "email,asc")
                )
        );
    }

    private void stub() {
        when(repository.findActiveByNutritionistAccount(any(), any(), any()))
                .thenReturn(new PageResult<>(List.of(), 0, 20, 0, 0));
    }
}
