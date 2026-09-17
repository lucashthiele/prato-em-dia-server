package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.ListFoodsInput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

public interface ListFoods extends UseCase<ListFoodsInput, PageResult<FoodOutput>> {}
