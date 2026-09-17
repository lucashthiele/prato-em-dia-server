package com.ulvratech.pratoemdia.server.features.catalog.application.port.in;

import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsInput;
import com.ulvratech.pratoemdia.server.features.catalog.application.dto.SyncImportedFoodsOutput;

public interface SyncImportedFoods {
  SyncImportedFoodsOutput execute(SyncImportedFoodsInput input);
}
