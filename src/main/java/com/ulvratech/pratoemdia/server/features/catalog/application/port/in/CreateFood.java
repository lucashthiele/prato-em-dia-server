package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface CreateFood extends UseCase<CreateFoodInput, FoodOutput> {}
