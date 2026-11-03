package com.ulvratech.pratoemdia.server.features.admin.presentation.web.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNutritionistRequest(
        @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.") @Size(max = 320, message = "O e-mail deve ter no máximo 320 caracteres.") String email,
        @NotBlank(message = "Informe o nome completo.") @Size(max = 160, message = "O nome completo deve ter no máximo 160 caracteres.") String fullName,
        @Size(max = 80, message = "O registro profissional deve ter no máximo 80 caracteres.") String registrationNumber) {
}
