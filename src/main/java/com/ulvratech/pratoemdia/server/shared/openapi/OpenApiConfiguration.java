package com.ulvratech.pratoemdia.server.shared.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
    info = @Info(
        title = "Prato em Dia API",
        version = "v1",
        description = "API para gestão de perfis, pacientes, alimentos e prescrições."
    )
)
@SecurityScheme(
    name = OpenApiConfiguration.BEARER_AUTH,
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Informe o access token JWT retornado por POST /api/v1/auth/login."
)
public class OpenApiConfiguration {
    public static final String BEARER_AUTH = "bearerAuth";

    private OpenApiConfiguration() {
    }
}
