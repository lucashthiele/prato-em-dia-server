package com.ulvratech.pratoemdia.server.features.admin.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChangeUserStatusRequest(
        @NotBlank(message = "Informe o status.") @EnumValue(enumClass = UserAccountStatus.class, message = "O status informado não é válido.") String status,
        @Schema(description = "Só vale ao desativar uma conta NUTRITIONIST: encerra, na mesma transação, todos os vínculos ativos dela (e arquiva as prescrições desses vínculos). Para outro papel ou para ativação, a resposta é 400 END_RELATIONSHIPS_NOT_APPLICABLE. Padrão false.", defaultValue = "false") Boolean endRelationships) {
}
