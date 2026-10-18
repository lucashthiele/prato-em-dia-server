package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.NutrientTotal;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Deleted;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Execution;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Item;
import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionItemModel;
import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionModel;
import java.util.HashMap;
import org.springframework.stereotype.Component;

@Component
public class MealExecutionMapper {
    public Execution toExecution(MealExecutionModel model) {
        var items = model.getItems().stream().map(this::toItem).toList();
        var totals = new HashMap<String, NutrientTotal>();
        for (var item : items)
            item.nutrients().forEach(
                    (code, value) -> totals.merge(
                            code, value,
                            (a, b) -> new NutrientTotal(a.amount().add(b.amount()), a.unit())
                    )
            );
        return new Execution(
                model.getId(), model.getVersion(), model.getDate(), model.getTime(),
                model.getMealType(), model.getPlannedMealId(), model.getWeeklyPlanId(),
                model.getPrescriptionVersionId(), model.getMode().name(), items, totals,
                model.getCreatedAt(), model.getUpdatedAt(), model.getDeletedAt(),
                model.getRestoreExpiresAt()
        );
    }

    public Deleted toDeleted(MealExecutionModel model) {
        return new Deleted(
                model.getId(), model.getVersion(), model.getRestoreToken(),
                model.getRestoreExpiresAt()
        );
    }

    private Item toItem(MealExecutionItemModel model) {
        var nutrients = new HashMap<String, NutrientTotal>();
        model.getNutrients().forEach(
                snapshot -> nutrients.put(
                        snapshot.getNutrientCode(),
                        new NutrientTotal(snapshot.getAmount(), snapshot.getUnit())
                )
        );
        return new Item(
                model.getId(), model.getFoodId(), model.getFoodPortionId(), model.getUnit(),
                model.getQuantity(), model.getMultiplier(), model.getPlannedItemId(),
                model.getAllowedSubstitutionId(), model.getGrams(), model.getKcal(), nutrients
        );
    }
}
