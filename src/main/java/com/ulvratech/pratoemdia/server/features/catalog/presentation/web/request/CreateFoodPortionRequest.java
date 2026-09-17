package com.ulvratech.pratoemdia.server.features.catalog.presentation.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFoodPortionRequest(
        @NotBlank(message = "Informe a descrição da porção.") @Size(max = 100, message = "A descrição deve ter no máximo 100 caracteres.") String label,

        @NotNull(message = "Informe a quantidade.") @DecimalMin(value = "0.001", message = "A quantidade deve ser positiva.") @Digits(integer = 9, fraction = 3, message = "A quantidade deve possuir até 9 inteiros e 3 decimais.") BigDecimal quantity,

        @NotBlank(message = "Informe a unidade.") @Size(max = 32, message = "A unidade deve ter no máximo 32 caracteres.") String unit,

        @DecimalMin(value = "0.001", message = "O equivalente em gramas deve ser positivo.") @Digits(integer = 9, fraction = 3, message = "O equivalente em gramas deve possuir até 9 inteiros e 3 decimais.") BigDecimal gramsEquivalent) {
}
