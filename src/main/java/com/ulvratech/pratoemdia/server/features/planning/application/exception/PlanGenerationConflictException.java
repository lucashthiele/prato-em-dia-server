package com.ulvratech.pratoemdia.server.features.planning.application.exception;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;

/** Outra geração do plano ocupou os mesmos dias primeiro (ADR 0002). */
public class PlanGenerationConflictException extends CodedApplicationException {
    public PlanGenerationConflictException() {
        super("WEEKLY_PLAN_GENERATION_CONFLICT");
    }
}
