package com.ulvratech.pratoemdia.server.features.identity.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.shared.annotations.EnumValue;
import jakarta.validation.constraints.NotEmpty;

public record CreateUserAccountRequest(
    @NotEmpty 
    String email,
    
    @NotEmpty 
    String password,
    
    @NotEmpty 
    @EnumValue(
        enumClass = UserAccountRole.class, 
        message = "Nível de acesso incorreto"
    ) 
    String role
) {
}
