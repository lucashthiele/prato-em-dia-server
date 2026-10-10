package com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.Food;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.mapper.FoodMapper;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.model.FoodModel;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FoodRepositoryAdapterTest {
    @Test
    void findsActiveFoodsWithStableNameAndIdOrderingAndPreservesPagination() {
        var repository = mock(FoodJpaRepository.class);
        var mapper = mock(FoodMapper.class);
        var adapter = new FoodRepositoryAdapter(repository, mapper);
        var model = mock(FoodModel.class);
        var food = mock(Food.class);
        var repositoryPage = new PageImpl<>(
                List.of(model),
                org.springframework.data.domain.PageRequest.of(1, 3), 8
        );
        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(repository.findAllByActiveTrue(pageableCaptor.capture())).thenReturn(repositoryPage);
        when(mapper.toDomain(model)).thenReturn(food);

        var result = adapter.findActive(new PageRequest(1, 3));

        assertEquals(List.of(food), result.content());
        assertEquals(1, result.page());
        assertEquals(3, result.size());
        assertEquals(8, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(
                List.of(
                        new Sort.Order(Sort.Direction.ASC, "name"),
                        new Sort.Order(Sort.Direction.ASC, "id")
                ), pageableCaptor.getValue().getSort().toList()
        );
    }
}
