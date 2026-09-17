package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodDetailsOutput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.FoodQueryInput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface GetFood extends UseCase<FoodQueryInput, FoodDetailsOutput> {}
