package com.ulvratech.pratoemdia.server.features.catalog.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ListFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.port.in.ListFoods;
import com.ulvratech.pratoemdia.server.features.catalog.domain.port.out.FoodRepository;
import com.ulvratech.pratoemdia.server.features.catalog.domain.FoodNameNormalizer;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.stereotype.Service;

@Service
public class ListFoodsUseCase implements ListFoods {
    private final FoodRepository repository;

    public ListFoodsUseCase(FoodRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<FoodOutput> execute(ListFoodsInput input) {
        var term = FoodNameNormalizer.normalize(input.q());
        if (term != null && term.isBlank())
            throw new IllegalArgumentException("Search term must not be blank");
        var page = term == null
                ? repository.findActive(new PageRequest(input.page(), input.size()))
                : repository.searchActive(term, new PageRequest(input.page(), input.size()));
        return new PageResult<>(
                page.content().stream().map(FoodOutput::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages()
        );
    }
}
