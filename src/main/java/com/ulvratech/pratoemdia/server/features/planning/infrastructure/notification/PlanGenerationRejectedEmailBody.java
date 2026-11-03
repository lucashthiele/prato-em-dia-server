package com.ulvratech.pratoemdia.server.features.planning.infrastructure.notification;

final class PlanGenerationRejectedEmailBody {
    static final String SUBJECT = "Não foi possível gerar seu plano semanal";

    private PlanGenerationRejectedEmailBody() {
    }

    static String build(String patientName) {
        return "Olá, " + patientName + ".\n\n"
                + "Não conseguimos gerar seu plano semanal automaticamente porque a prescrição atual não permite"
                + " montar o plano. Isso não se resolve tentando novamente.\n\n"
                + "Fale com o seu nutricionista e peça a revisão da prescrição. Assim que ela for ajustada,"
                + " você poderá gerar o plano pelo aplicativo.";
    }
}
