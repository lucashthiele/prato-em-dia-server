package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.CreateFoodPortionsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodPortionOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;
import java.util.List;

public interface CreateFoodPortion
    extends UseCase<CreateFoodPortionsInput, List<FoodPortionOutput>> {}
