package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.UpdateFoodInput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface UpdateFood extends UseCase<UpdateFoodInput, FoodOutput> {
}
