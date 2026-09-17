package com.ulvratech.pratoemdia.server.features.identity.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import jakarta.validation.constraints.*;

public record CreateUserAccountRequest(
    @NotEmpty(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.")
        String email,
    @NotBlank(message = "Informe a senha.")
        @Size(min = 12, max = 72, message = "A senha deve ter entre 12 e 72 caracteres.")
        String password,
    @NotBlank(message = "Informe o tipo de conta.")
        @EnumValue(
            enumClass = UserAccountRole.class,
            message = "O tipo de conta informado não é válido.")
        String role,
    @Size(max = 160, message = "O nome completo deve ter no máximo 160 caracteres.")
        String fullName) {
  @AssertTrue(message = "Informe o nome completo para paciente ou nutricionista.")
  public boolean isFullNameValidForRole() {
    return "ADMIN".equals(role) || (fullName != null && !fullName.isBlank());
  }
}
