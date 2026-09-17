package com.ulvratech.pratoemdia.server.features.profiles.presentation.web.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateCurrentProfileRequest(
        @NotBlank(message = "Informe o nome completo.") @Size(max = 160, message = "O nome completo deve ter no máximo 160 caracteres.") String fullName,

        @Size(max = 32, message = "O telefone deve ter no máximo 32 caracteres.") String phone,

        @Size(max = 80, message = "O registro profissional deve ter no máximo 80 caracteres.") String registrationNumber,

        String bio,

        @PastOrPresent(message = "A data de nascimento não pode estar no futuro.") LocalDate birthDate) {
}
