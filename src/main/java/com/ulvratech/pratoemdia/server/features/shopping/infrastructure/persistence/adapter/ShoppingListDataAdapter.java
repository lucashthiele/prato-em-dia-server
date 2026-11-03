package com.ulvratech.pratoemdia.server.features.shopping.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.shopping.application.exception.ShoppingPlanNotFoundException;
import com.ulvratech.pratoemdia.server.features.shopping.application.port.out.ShoppingListData;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.PlannedLine;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingList;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingListConsolidation;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingListCreation;
import com.ulvratech.pratoemdia.server.features.shopping.domain.entity.ShoppingListItem;
import com.ulvratech.pratoemdia.server.features.shopping.infrastructure.persistence.model.ShoppingListItemModel;
import com.ulvratech.pratoemdia.server.features.shopping.infrastructure.persistence.model.ShoppingListModel;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter.PlanningQueryAdapter;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ShoppingListDataAdapter implements ShoppingListData {
    private final ShoppingListJpaRepository lists;
    private final PlanningQueryAdapter planning;
    private final Clock clock;

    ShoppingListDataAdapter(ShoppingListJpaRepository lists, PlanningQueryAdapter planning, Clock clock) {
        this.lists = lists;
        this.planning = planning;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ShoppingList> findActive(UUID planId) {
        return lists.findByWeeklyPlanIdAndActiveTrue(planId).map(ShoppingListDataAdapter::toDomain);
    }

    @Override
    @Transactional
    public ShoppingListCreation createIfAbsent(UUID planId, UUID patientId) {
        // The plan row lock serializes concurrent creators; the partial unique index backs it up.
        if (planning.lockActivePlanOwner(planId).filter(patientId::equals).isEmpty())
            throw new ShoppingPlanNotFoundException();
        var existing = lists.findByWeeklyPlanIdAndActiveTrue(planId);
        if (existing.isPresent())
            return new ShoppingListCreation(false, toDomain(existing.get()));
        var items = ShoppingListConsolidation.consolidate(
                planning.plannedItems(planId).stream()
                        .map(line -> new PlannedLine(line.foodId(), line.foodPortionId(), line.unit(), line.quantity()))
                        .toList()
        );
        var model = ShoppingListModel.builder()
                .id(UUID.randomUUID()).weeklyPlanId(planId).patientId(patientId).active(true)
                .createdAt(clock.instant().truncatedTo(ChronoUnit.MICROS)).build();
        IntStream.range(0, items.size()).forEach(index -> {
            var item = items.get(index);
            model.getItems().add(
                    ShoppingListItemModel.builder()
                            .id(UUID.randomUUID()).list(model).foodId(item.foodId())
                            .foodPortionId(item.foodPortionId()).unit(item.unit()).quantity(item.quantity())
                            .order(index).build()
            );
        });
        return new ShoppingListCreation(true, toDomain(lists.saveAndFlush(model)));
    }

    private static ShoppingList toDomain(ShoppingListModel model) {
        return new ShoppingList(
                model.getId(), model.getWeeklyPlanId(), model.getPatientId(), model.getCreatedAt(),
                model.getItems().stream()
                        .map(
                                item -> new ShoppingListItem(
                                        item.getFoodId(), item.getFoodPortionId(), item.getUnit(), item.getQuantity()
                                )
                        ).toList()
        );
    }
}
