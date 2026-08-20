package com.ulvratech.pratoemdia.server.features.identity.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record CreateUserAccountRequest(
    @NotEmpty(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    String email,
    
    @NotEmpty(message = "Informe a senha.")
    String password,
    
    @NotEmpty(message = "Informe o tipo de conta.")
    @EnumValue(
        enumClass = UserAccountRole.class, 
        message = "O tipo de conta informado não é válido."
    ) 
    String role
) {
}
