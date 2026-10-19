package com.ulvratech.pratoemdia.server.features.planning.infrastructure.notification;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;
import com.ulvratech.pratoemdia.server.features.planning.application.usecase.WeeklyPlanningUseCase;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

final class PlanGeneratedEmailBody {
    static final String SUBJECT = "Seu plano semanal está pronto";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DEADLINE = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private PlanGeneratedEmailBody() {
    }

    static String build(String patientName, PlanGeneratedNotice notice) {
        var origin = notice.automatic()
                ? "Geramos seu plano semanal automaticamente."
                : "Seu plano semanal foi regenerado a seu pedido.";
        var deadline = notice.confirmationDeadline().atZone(WeeklyPlanningUseCase.ZONE).format(DEADLINE);
        return "Olá, " + patientName + ".\n\n"
                + origin + "\n\n"
                + "Período: " + notice.periodStart().format(DAY) + " a " + notice.periodEnd().format(DAY) + "\n"
                + "Refeições no plano: " + notice.mealCount() + "\n"
                + "Refeições por dia: " + notice.mealsPerDay() + "\n"
                + "Tipos de refeição: " + notice.dailyMealTypes().stream()
                        .map(PlanGeneratedEmailBody::label)
                        .collect(Collectors.joining(", "))
                + "\n\n"
                + "Você pode ajustar as refeições no app até " + deadline
                + ". Depois desse prazo, o plano é confirmado automaticamente.";
    }

    private static String label(MealType type) {
        return switch (type) {
            case BREAKFAST -> "Café da manhã";
            case LUNCH -> "Almoço";
            case SNACK -> "Lanche";
            case DINNER -> "Jantar";
        };
    }
}
