package com.ulvratech.pratoemdia.server.features.shopping.application.usecase;

import com.ulvratech.pratoemdia.server.features.shopping.application.dto.ShoppingListOutput;
import com.ulvratech.pratoemdia.server.features.shopping.application.dto.ShoppingListOutput.Actor;
import com.ulvratech.pratoemdia.server.features.shopping.application.dto.ShoppingListOutput.ItemOutput;
import com.ulvratech.pratoemdia.server.features.shopping.application.exception.ShoppingAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.shopping.application.exception.ShoppingListNotFoundException;
import com.ulvratech.pratoemdia.server.features.shopping.application.exception.ShoppingPlanNotFoundException;
import com.ulvratech.pratoemdia.server.features.shopping.application.port.in.ShoppingListOperations;
import com.ulvratech.pratoemdia.server.features.shopping.application.port.out.ShoppingListData;
import com.ulvratech.pratoemdia.server.features.shopping.application.port.out.ShoppingReferences;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingList;
import java.util.HashSet;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Not transactional: each port call owns its transaction. */
@Service
public class ShoppingListUseCase implements ShoppingListOperations {
    private final ShoppingListData data;
    private final ShoppingReferences references;

    public ShoppingListUseCase(ShoppingListData data, ShoppingReferences references) {
        this.data = data;
        this.references = references;
    }

    @Override
    public ShoppingListOutput.Result create(Actor actor, UUID planId) {
        var patient = ownedPlanPatient(actor, planId);
        var created = data.createIfAbsent(planId, patient);
        return new ShoppingListOutput.Result(created.created(), output(created.list()));
    }

    @Override
    public ShoppingListOutput get(Actor actor, UUID planId) {
        ownedPlanPatient(actor, planId);
        return data.findActive(planId).map(this::output).orElseThrow(ShoppingListNotFoundException::new);
    }

    /** A plan that does not exist, was deleted or belongs to someone else is "not found". */
    private UUID ownedPlanPatient(Actor actor, UUID planId) {
        if (actor == null || !"PATIENT".equals(actor.role()))
            throw new ShoppingAccessDeniedException();
        var patient = references.patientId(actor.accountId()).orElseThrow(ShoppingAccessDeniedException::new);
        var owner = references.activePlanOwner(planId).orElseThrow(ShoppingPlanNotFoundException::new);
        if (!owner.equals(patient))
            throw new ShoppingPlanNotFoundException();
        return patient;
    }

    private ShoppingListOutput output(ShoppingList list) {
        var foodIds = new HashSet<UUID>();
        var portionIds = new HashSet<UUID>();
        list.items().forEach(item -> {
            foodIds.add(item.foodId());
            if (item.foodPortionId() != null)
                portionIds.add(item.foodPortionId());
        });
        var foods = references.foods(foodIds);
        var portions = references.portions(portionIds);
        return new ShoppingListOutput(
                list.id(), list.weeklyPlanId(), list.createdAt(),
                list.items().stream().map(item -> {
                    var food = foods.get(item.foodId());
                    var portion = item.foodPortionId() == null ? null : portions.get(item.foodPortionId());
                    return new ItemOutput(
                            item.foodId(), food == null ? null : food.getName(), item.foodPortionId(),
                            portion == null ? null : portion.getLabel(), item.unit(), item.quantity()
                    );
                }).toList()
        );
    }
}
