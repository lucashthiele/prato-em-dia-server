package com.ulvratech.pratoemdia.server.features.admin.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import jakarta.validation.constraints.NotBlank;

public record ChangeUserStatusRequest(
        @NotBlank(message = "Informe o status.") @EnumValue(enumClass = UserAccountStatus.class, message = "O status informado não é válido.") String status) {
}
