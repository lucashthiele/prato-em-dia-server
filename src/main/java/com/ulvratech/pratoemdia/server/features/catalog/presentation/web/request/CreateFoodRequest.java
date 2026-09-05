package com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateFoodRequest(
    @NotBlank(message = "Informe o nome do alimento.")
    @Size(max = 180, message = "O nome deve ter no máximo 180 caracteres.")
    String name,

    @Size(max = 100, message = "A categoria deve ter no máximo 100 caracteres.")
    String category,

    @NotBlank(message = "Informe a unidade padrão.")
    @Size(max = 32, message = "A unidade padrão deve ter no máximo 32 caracteres.")
    String defaultUnit,

    @Size(max = 4000, message = "As observações devem ter no máximo 4000 caracteres.")
    String notes,

    @Size(max = 4000, message = "O modo de preparo deve ter no máximo 4000 caracteres.")
    String preparationMethod,

    List<@NotNull(message = "A porção não pode ser nula.") @Valid CreateFoodPortionRequest> portions
) {
    public CreateFoodRequest {
        portions = portions == null ? List.of() : portions;
    }
}
