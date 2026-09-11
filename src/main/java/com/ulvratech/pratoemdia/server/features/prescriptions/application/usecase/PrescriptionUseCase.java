package com.ulvratech.pratoemdia.server.features.prescriptions.application.usecase;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionContentInput.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionInputs.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.exception.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.out.PrescriptionReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.*;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PrescriptionUseCase implements PrescriptionCommands, PrescriptionQueries {
    private final PrescriptionRepository repository;
    private final PrescriptionReferences references;
    private final Clock clock;

    public PrescriptionUseCase(PrescriptionRepository repository,
                               PrescriptionReferences references, Clock clock) {
        this.repository = repository;
        this.references = references;
        this.clock = clock;
    }

    @Override
    public PrescriptionOutput create(Create input) {
        UUID nutritionistId = requireNutritionist(input.actor());
        requireRelationship(nutritionistId, input.patientId());
        if (!repository.findByNutritionistAndPatient(nutritionistId, input.patientId()).isEmpty())
            throw new PrescriptionConflictException("An active prescription already exists for this patient");
        validateReferences(input.content());
        var prescription = Prescription.create(input.patientId(), nutritionistId,
            input.actor().userAccountId(), input.content().title(), input.content().notes(),
            meals(input.content()));
        return PrescriptionOutput.from(repository.save(prescription));
    }

    @Override
    public PrescriptionOutput.VersionOutput createVersion(PrescriptionQuery input) {
        var prescription = requirePrescription(input.prescriptionId());
        requireAuthor(prescription, input.actor());
        try {
            prescription.addDraft(input.actor().userAccountId());
            return newest(repository.save(prescription));
        } catch (IllegalStateException exception) {
            throw conflict(exception);
        }
    }

    @Override
    public PrescriptionOutput.VersionOutput updateVersion(UpdateVersion input) {
        var prescription = requireByVersion(input.versionId());
        requireAuthor(prescription, input.actor());
        validateReferences(input.content());
        try {
            prescription.requireVersion(input.versionId()).replaceContent(
                input.content().title(), input.content().notes(), meals(input.content()));
            return version(repository.save(prescription), input.versionId());
        } catch (IllegalStateException exception) {
            throw conflict(exception);
        }
    }

    @Override
    public PrescriptionOutput.VersionOutput publish(VersionQuery input) {
        var prescription = requireByVersion(input.versionId());
        requireAuthor(prescription, input.actor());
        validateReferences(toInput(prescription.requireVersion(input.versionId())));
        try {
            prescription.publish(input.versionId(), Instant.now(clock));
            return version(repository.save(prescription), input.versionId());
        } catch (IllegalStateException exception) {
            throw conflict(exception);
        }
    }

    @Override
    public PrescriptionOutput.VersionOutput archive(VersionQuery input) {
        var prescription = requireByVersion(input.versionId());
        requireAuthor(prescription, input.actor());
        try {
            prescription.requireVersion(input.versionId()).archive(Instant.now(clock));
            return version(repository.save(prescription), input.versionId());
        } catch (IllegalStateException exception) {
            throw conflict(exception);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PrescriptionOutput> list(PatientQuery input) {
        UUID nutritionistId = requireNutritionist(input.actor());
        requireRelationship(nutritionistId, input.patientId());
        return repository.findByNutritionistAndPatient(nutritionistId, input.patientId()).stream()
            .map(PrescriptionOutput::from).toList();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public PrescriptionOutput get(PrescriptionQuery input) {
        var prescription = requirePrescription(input.prescriptionId());
        requireReader(prescription, input.actor(), true);
        return outputVisibleTo(PrescriptionOutput.from(prescription), input.actor());
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<PrescriptionOutput.VersionOutput> listVersions(PrescriptionQuery input) {
        var prescription = requirePrescription(input.prescriptionId());
        requireReader(prescription, input.actor(), false);
        return outputVisibleTo(PrescriptionOutput.from(prescription), input.actor()).versions();
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public PrescriptionOutput.VersionOutput getVersion(VersionQuery input) {
        var prescription = requireByVersion(input.versionId());
        requireReader(prescription, input.actor(), false);
        if ("PATIENT".equals(input.actor().role())
            && prescription.requireVersion(input.versionId()).getPublishedAt() == null)
            throw new PrescriptionNotFoundException();
        return version(prescription, input.versionId());
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public PrescriptionOutput.VersionOutput current(Actor actor) {
        if (!"PATIENT".equals(actor.role())) throw new PrescriptionAccessDeniedException();
        UUID patientId = references.patientProfileId(actor.userAccountId())
            .orElseThrow(PrescriptionAccessDeniedException::new);
        var prescription = repository.findCurrentByPatient(patientId)
            .orElseThrow(PrescriptionNotFoundException::new);
        return PrescriptionOutput.from(prescription).versions().stream()
            .filter(v -> v.status().name().equals("PUBLISHED"))
            .findFirst().orElseThrow(PrescriptionNotFoundException::new);
    }

    private void requireReader(Prescription prescription, Actor actor, boolean allowAdmin) {
        if (allowAdmin && "ADMIN".equals(actor.role())) return;
        if ("NUTRITIONIST".equals(actor.role()) && references.nutritionistProfileId(actor.userAccountId())
            .filter(prescription.getNutritionistId()::equals)
            .filter(id -> references.hasActiveRelationship(id, prescription.getPatientId()))
            .isPresent()) return;
        if ("PATIENT".equals(actor.role()) && references.patientProfileId(actor.userAccountId())
            .filter(prescription.getPatientId()::equals).isPresent()) return;
        throw new PrescriptionAccessDeniedException();
    }

    private void requireAuthor(Prescription prescription, Actor actor) {
        UUID nutritionistId = requireNutritionist(actor);
        if (!prescription.getNutritionistId().equals(nutritionistId))
            throw new PrescriptionAccessDeniedException();
        requireRelationship(nutritionistId, prescription.getPatientId());
    }

    private UUID requireNutritionist(Actor actor) {
        if (!"NUTRITIONIST".equals(actor.role())) throw new PrescriptionAccessDeniedException();
        return references.nutritionistProfileId(actor.userAccountId())
            .orElseThrow(PrescriptionAccessDeniedException::new);
    }

    private void requireRelationship(UUID nutritionistId, UUID patientId) {
        if (!references.hasActiveRelationship(nutritionistId, patientId))
            throw new PrescriptionAccessDeniedException();
    }

    private Prescription requirePrescription(UUID id) {
        return repository.findById(id).orElseThrow(PrescriptionNotFoundException::new);
    }

    private Prescription requireByVersion(UUID id) {
        return repository.findByVersionId(id).orElseThrow(PrescriptionNotFoundException::new);
    }

    private void validateReferences(PrescriptionContentInput input) {
        input.meals().forEach(meal -> meal.items().forEach(item -> {
            validateFood(item.foodId(), item.foodPortionId());
            item.substitutions().forEach(sub -> validateFood(sub.foodId(), sub.foodPortionId()));
        }));
    }

    private void validateFood(UUID foodId, UUID portionId) {
        if (foodId == null || !references.isActiveFood(foodId))
            throw new InvalidPrescriptionReferenceException("Food is missing or inactive");
        if (portionId != null && !references.portionBelongsToFood(portionId, foodId))
            throw new InvalidPrescriptionReferenceException("Food portion does not belong to the selected food");
    }

    private List<PrescribedMeal> meals(PrescriptionContentInput input) {
        return input.meals().stream().map(this::meal).toList();
    }

    private PrescribedMeal meal(MealInput meal) {
        return PrescribedMeal.create(meal.name(), meal.order(), meal.suggestedTime(), meal.notes(),
            meal.items().stream().map(this::item).toList());
    }

    private PrescribedMealItem item(ItemInput item) {
        return PrescribedMealItem.create(item.foodId(), item.foodPortionId(), item.quantity(),
            item.unit(), item.order(), item.notes(), item.substitutions().stream()
                .map(this::substitution).toList());
    }

    private AllowedSubstitution substitution(SubstitutionInput input) {
        return AllowedSubstitution.create(input.foodId(), input.foodPortionId(), input.quantity(),
            input.unit(), input.notes());
    }

    private PrescriptionContentInput toInput(PrescriptionVersion version) {
        return new PrescriptionContentInput(version.getTitle(), version.getNotes(),
            version.getMeals().stream().map(meal -> new MealInput(meal.name(), meal.order(),
                meal.suggestedTime(), meal.notes(), meal.items().stream().map(item ->
                    new ItemInput(item.foodId(), item.foodPortionId(), item.quantity(), item.unit(),
                        item.order(), item.notes(), item.substitutions().stream().map(sub ->
                            new SubstitutionInput(sub.foodId(), sub.foodPortionId(), sub.quantity(),
                                sub.unit(), sub.notes())).toList())).toList())).toList());
    }

    private PrescriptionOutput.VersionOutput newest(Prescription prescription) {
        return PrescriptionOutput.from(prescription).versions().getFirst();
    }

    private PrescriptionOutput.VersionOutput version(Prescription prescription, UUID id) {
        return PrescriptionOutput.VersionOutput.from(prescription.requireVersion(id));
    }

    private PrescriptionOutput outputVisibleTo(PrescriptionOutput output, Actor actor) {
        if (!"PATIENT".equals(actor.role())) return output;
        return new PrescriptionOutput(output.id(), output.patientId(), output.nutritionistId(),
            output.status(), output.createdAt(), output.updatedAt(), output.versions().stream()
                .filter(version -> version.publishedAt() != null).toList());
    }

    private PrescriptionConflictException conflict(IllegalStateException exception) {
        return new PrescriptionConflictException(exception.getMessage());
    }
}
