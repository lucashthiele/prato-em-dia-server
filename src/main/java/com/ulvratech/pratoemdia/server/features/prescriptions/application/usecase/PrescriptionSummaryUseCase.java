package com.ulvratech.pratoemdia.server.features.prescriptions.application.usecase;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.PatientQuery;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.HistoryEntry;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.ItemSummary;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.Macros;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.MealSummary;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.SubstitutionSummary;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.TargetSummary;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionSummary.VersionSummary;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.PrescriptionAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in.PrescriptionSummaryQueries;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescriptionVersion;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.service.MacroSum;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.service.SummaryCatalog;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PrescriptionSummaryUseCase implements PrescriptionSummaryQueries {
    private static final String KCAL = "energy_kcal";
    private static final String PROTEIN = "protein";
    private static final String CARBOHYDRATE = "carbohydrate";
    private static final String LIPIDS = "lipids";

    private final PrescriptionRepository repository;
    private final PrescriptionReferences references;

    public PrescriptionSummaryUseCase(
            PrescriptionRepository repository, PrescriptionReferences references
    ) {
        this.repository = repository;
        this.references = references;
    }

    @Override
    public PrescriptionSummary summary(PatientQuery input) {
        if (input.actor() == null || !"NUTRITIONIST".equals(input.actor().role()))
            throw new PrescriptionAccessDeniedException();
        UUID nutritionistId = references.nutritionistProfileId(input.actor().userAccountId())
                .orElseThrow(PrescriptionAccessDeniedException::new);
        if (!references.hasActiveRelationship(nutritionistId, input.patientId()))
            throw new PrescriptionAccessDeniedException();
        var prescription = repository.findByNutritionistAndPatient(nutritionistId, input.patientId())
                .stream().findFirst();
        if (prescription.isEmpty())
            return new PrescriptionSummary(null, null, null, null, List.of());
        var versions = prescription.get().getVersions().stream()
                .sorted(Comparator.comparingInt(PrescriptionVersion::getNumber).reversed()).toList();
        var current = versions.stream()
                .filter(version -> version.getStatus() == PrescriptionVersionStatus.PUBLISHED).findFirst();
        var draft = versions.stream()
                .filter(version -> version.getStatus() == PrescriptionVersionStatus.DRAFT).findFirst();
        var catalog = SummaryCatalog.load(
                references, java.util.stream.Stream.concat(current.stream(), draft.stream()).toList()
        );
        return new PrescriptionSummary(
                prescription.get().getId(), prescription.get().getPlanningMode(),
                current.map(version -> version(version, catalog)).orElse(null),
                draft.map(version -> version(version, catalog)).orElse(null),
                versions.stream().map(PrescriptionSummaryUseCase::history).toList()
        );
    }

    private VersionSummary version(PrescriptionVersion version, SummaryCatalog catalog) {
        var meals = version.getMeals().stream().sorted(Comparator.comparingInt(PrescribedMeal::order))
                .map(meal -> meal(meal, catalog)).toList();
        var totals = new MacroSum();
        version.getMeals()
                .forEach(meal -> meal.items().forEach(item -> totals.add(macros(item, catalog, false))));
        return new VersionSummary(
                version.getId(), version.getNumber(), version.getStatus(), version.getTitle(),
                version.getNotes(), version.getCreatedAt(), version.getPublishedAt(), totals.result(),
                version.getNutritionTargets().stream()
                        .map(target -> new TargetSummary(target.nutrientCode(), target.amount(), target.unit()))
                        .toList(),
                meals
        );
    }

    private MealSummary meal(PrescribedMeal meal, SummaryCatalog catalog) {
        var items = meal.items().stream().sorted(Comparator.comparingInt(PrescribedMealItem::order))
                .map(item -> item(item, catalog)).toList();
        var totals = new MacroSum();
        meal.items().forEach(item -> totals.add(macros(item, catalog, false)));
        return new MealSummary(
                meal.id(), meal.name(), meal.mealType(), meal.order(), meal.suggestedTime(), meal.notes(),
                totals.result(), items
        );
    }

    private ItemSummary item(PrescribedMealItem item, SummaryCatalog catalog) {
        var portion = catalog.portion(item.foodPortionId());
        return new ItemSummary(
                item.id(), item.foodId(), catalog.foodName(item.foodId()), item.quantity(), item.unit(),
                portion == null ? null : portion.getLabel(), item.notes(), macros(item, catalog, true),
                item.substitutions().stream().map(substitution -> {
                    var substitutePortion = catalog.portion(substitution.foodPortionId());
                    return new SubstitutionSummary(
                            substitution.id(), substitution.foodId(), catalog.foodName(substitution.foodId()),
                            substitution.quantity(), substitution.unit(),
                            substitutePortion == null ? null : substitutePortion.getLabel(),
                            substitution.notes()
                    );
                }).toList()
        );
    }

    private Macros macros(PrescribedMealItem item, SummaryCatalog catalog, boolean rounded) {
        var portion = catalog.portion(item.foodPortionId());
        var kcal = scaled(portion, KCAL, item.quantity());
        var protein = scaled(portion, PROTEIN, item.quantity());
        var carbohydrate = scaled(portion, CARBOHYDRATE, item.quantity());
        var lipids = scaled(portion, LIPIDS, item.quantity());
        boolean complete = kcal != null && protein != null && carbohydrate != null && lipids != null;
        return rounded
                ? new Macros(
                        MacroSum.round(kcal), MacroSum.round(protein), MacroSum.round(carbohydrate),
                        MacroSum.round(lipids), complete
                )
                : new Macros(kcal, protein, carbohydrate, lipids, complete);
    }

    private static BigDecimal scaled(FoodPortion portion, String code, BigDecimal quantity) {
        return portion == null ? null : portion.scaledNutrient(code, quantity).orElse(null);
    }

    private static HistoryEntry history(PrescriptionVersion version) {
        return new HistoryEntry(
                version.getId(), version.getNumber(), version.getStatus(), version.getTitle(),
                version.getCreatedAt(), version.getPublishedAt(), version.getArchivedAt()
        );
    }
}
