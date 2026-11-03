package com.ulvratech.pratoemdia.server.shared.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "Prato em Dia API", version = "v1", description = "API que conecta nutricionistas e pacientes: perfis, vínculos, catálogo de alimentos, prescrições versionadas, planejamento semanal e registro do que foi consumido."))
@SecurityScheme(name = OpenApiConfiguration.COOKIE_AUTH, type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "access_token", description = "Sessão por cookie HttpOnly access_token, emitido por POST /api/v1/auth/login (ADR 0001). O navegador (e o Swagger UI, na mesma origem) o envia sozinho.")
public class OpenApiConfiguration {
    public static final String COOKIE_AUTH = "cookieAuth";
    /** @deprecated o Bearer deixou de ser aceito (ADR 0001); use {@link #COOKIE_AUTH}. */
    @Deprecated
    public static final String BEARER_AUTH = COOKIE_AUTH;

    private OpenApiConfiguration() {
    }
}
