package com.ulvratech.pratoemdia.server.features.shopping.application.port.in;

import com.ulvratech.pratoemdia.server.features.shopping.application.dto.ShoppingListOutput;
import com.ulvratech.pratoemdia.server.features.shopping.application.dto.ShoppingListOutput.Actor;
import java.util.UUID;

public interface ShoppingListOperations {
    /** Creates the list from the plan's planned meals, or returns the active one. */
    ShoppingListOutput.Result create(Actor actor, UUID planId);

    /** The active list of the plan. */
    ShoppingListOutput get(Actor actor, UUID planId);
}
