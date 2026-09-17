package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.AllowedSubstitution;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescriptionVersion;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.AllowedSubstitutionModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescribedMealItemModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescribedMealModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescriptionModel;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescriptionVersionModel;
import org.springframework.stereotype.Component;
import java.util.ArrayList;

@Component
public class PrescriptionMapper {
    public PrescriptionModel toModel(Prescription domain) {
        var root = PrescriptionModel.builder().id(domain.getId()).patientId(domain.getPatientId())
                .nutritionistId(domain.getNutritionistId()).status(domain.getStatus())
                .createdAt(domain.getCreatedAt()).updatedAt(domain.getUpdatedAt())
                .version(domain.getPersistenceVersion()).build();
        root.setVersions(
                domain.getVersions().stream().map(version -> toModel(root, version))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        return root;
    }

    private PrescriptionVersionModel toModel(PrescriptionModel root, PrescriptionVersion version) {
        var model = PrescriptionVersionModel.builder().id(version.getId()).prescription(root)
                .number(version.getNumber()).status(version.getStatus()).title(version.getTitle())
                .notes(version.getNotes()).createdByUserId(version.getCreatedByUserId())
                .createdAt(version.getCreatedAt()).publishedAt(version.getPublishedAt())
                .archivedAt(version.getArchivedAt()).build();
        model.setMeals(
                version.getMeals().stream().map(meal -> toModel(model, meal))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        return model;
    }

    private PrescribedMealModel toModel(PrescriptionVersionModel version, PrescribedMeal meal) {
        var model = PrescribedMealModel.builder().id(meal.id()).version(version).name(meal.name())
                .order(meal.order()).suggestedTime(meal.suggestedTime()).notes(meal.notes())
                .createdAt(meal.createdAt()).build();
        model.setItems(
                meal.items().stream().map(item -> toModel(model, item))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        return model;
    }

    private PrescribedMealItemModel toModel(PrescribedMealModel meal, PrescribedMealItem item) {
        var model = PrescribedMealItemModel.builder().id(item.id()).meal(meal).foodId(item.foodId())
                .foodPortionId(item.foodPortionId()).quantity(item.quantity()).unit(item.unit())
                .order(item.order()).notes(item.notes()).build();
        model.setSubstitutions(
                item.substitutions().stream()
                        .map(
                                sub -> AllowedSubstitutionModel.builder().id(sub.id()).item(model).foodId(sub.foodId())
                                        .foodPortionId(sub.foodPortionId()).quantity(sub.quantity()).unit(sub.unit())
                                        .notes(sub.notes()).createdAt(sub.createdAt()).build()
                        )
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        return model;
    }

    public Prescription toDomain(PrescriptionModel model) {
        return new Prescription(
                model.getId(), model.getPatientId(), model.getNutritionistId(),
                model.getStatus(), model.getCreatedAt(), model.getUpdatedAt(), model.getVersion(),
                model.getVersions().stream().map(this::toDomain).toList()
        );
    }

    private PrescriptionVersion toDomain(PrescriptionVersionModel model) {
        return new PrescriptionVersion(
                model.getId(), model.getNumber(), model.getStatus(),
                model.getTitle(), model.getNotes(), model.getCreatedByUserId(), model.getCreatedAt(),
                model.getPublishedAt(), model.getArchivedAt(),
                model.getMeals().stream().map(this::toDomain).toList()
        );
    }

    private PrescribedMeal toDomain(PrescribedMealModel model) {
        return new PrescribedMeal(
                model.getId(), model.getName(), model.getOrder(),
                model.getSuggestedTime(), model.getNotes(), model.getCreatedAt(),
                model.getItems().stream().map(this::toDomain).toList()
        );
    }

    private PrescribedMealItem toDomain(PrescribedMealItemModel model) {
        return new PrescribedMealItem(
                model.getId(), model.getFoodId(), model.getFoodPortionId(),
                model.getQuantity(), model.getUnit(), model.getOrder(), model.getNotes(),
                model.getSubstitutions().stream().map(
                        sub -> new AllowedSubstitution(
                                sub.getId(), sub.getFoodId(), sub.getFoodPortionId(), sub.getQuantity(), sub.getUnit(),
                                sub.getNotes(), sub.getCreatedAt()
                        )
                ).toList()
        );
    }
}
