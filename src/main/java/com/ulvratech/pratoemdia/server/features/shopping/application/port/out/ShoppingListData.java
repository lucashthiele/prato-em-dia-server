package com.ulvratech.pratoemdia.server.features.shopping.application.port.out;

import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingList;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingListCreation;
import java.util.Optional;
import java.util.UUID;

public interface ShoppingListData {
    Optional<ShoppingList> findActive(UUID planId);

    /**
     * In one transaction: locks the plan, returns its active list or builds one from the plan's
     * planned meals.
     */
    ShoppingListCreation createIfAbsent(UUID planId, UUID patientId);
}
