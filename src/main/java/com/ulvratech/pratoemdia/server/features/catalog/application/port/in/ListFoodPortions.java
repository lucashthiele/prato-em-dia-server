package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodPortionOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodQueryInput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

import java.util.List;

public interface ListFoodPortions extends UseCase<FoodQueryInput, List<FoodPortionOutput>> {
}
