package com.ulvratech.pratoemdia.server.features.consent.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.consent.domain.enums.ConsentDocumentType;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import jakarta.validation.constraints.NotBlank;

public record AcceptConsentRequest(
        @NotBlank(message = "Informe o documento.") @EnumValue(enumClass = ConsentDocumentType.class, message = "O documento informado não é válido.") String documentType,
        @NotBlank(message = "Informe a versão.") String version) {
}
