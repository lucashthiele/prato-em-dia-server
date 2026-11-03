package com.ulvratech.pratoemdia.server.shared.errorcatalog;

import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Publica o envelope de erro e o catálogo de códigos no OpenAPI (LUC-50): o schema
 * {@code ApiErrorResponse}, as respostas {@code 4XX} e {@code 5XX} de toda operação, a tabela
 * legível em {@code info.description} e a lista estruturada em {@code info.x-error-catalog}.
 */
@Configuration(proxyBeanMethods = false)
public class ErrorCatalogOpenApiCustomizer {
    private static final String SCHEMA = "ApiErrorResponse";

    @Bean
    OpenApiCustomizer errorCatalogCustomizer(ErrorCatalog catalog) {
        return openApi -> {
            ModelConverters.getInstance().readAll(ApiErrorResponse.class)
                    .forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
            // O record de campo também precisa estar nos componentes para o $ref de errors[].
            ModelConverters.getInstance().readAll(
                    com.ulvratech.pratoemdia.server.features.identity.presentation.exception.dto.ApiFieldError.class
            ).forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));

            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(
                        path -> path.readOperations().forEach(operation -> {
                            var responses = operation.getResponses();
                            responses.addApiResponse(
                                    "4XX",
                                    errorResponse("Erro do cliente. Veja o catálogo de códigos em info.description.")
                            );
                            responses.addApiResponse(
                                    "5XX", errorResponse("Erro do servidor. O corpo não expõe detalhes internos.")
                            );
                        })
                );
            }

            var info = openApi.getInfo();
            info.setDescription(
                    (info.getDescription() == null ? "" : info.getDescription() + "\n\n") + markdownTable(catalog)
            );
            info.addExtension(
                    "x-error-catalog",
                    catalog.entries().stream().map(entry -> {
                        var map = new LinkedHashMap<String, Object>();
                        map.put("code", entry.code());
                        map.put("status", entry.status());
                        map.put("kind", entry.kind().name());
                        map.put("meaning", entry.meaning());
                        map.put("routes", entry.routes());
                        return map;
                    }).toList()
            );
        };
    }

    private static ApiResponse errorResponse(String description) {
        var schema = new Schema<>().$ref("#/components/schemas/" + SCHEMA);
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
    }

    private static String markdownTable(ErrorCatalog catalog) {
        var rows = catalog.entries().stream()
                .map(
                        entry -> "| `%s` | %s | %s | %s |".formatted(
                                entry.code(),
                                entry.status() == null ? "motivo" : entry.status(),
                                entry.meaning().replace("|", "\\|"),
                                entry.routes().stream().map(route -> "`" + route + "`")
                                        .collect(Collectors.joining(", "))
                        )
                )
                .collect(Collectors.joining("\n"));
        return """
                ## Catálogo de erros

                Toda resposta de erro usa o envelope `{status, code, message, errors[]}` (não é RFC 9457). \
                O cliente deve decidir pelo `code`, nunca pelo texto de `message`.

                | Código | Status | Significado | Rotas |
                |---|---|---|---|
                """ + rows;
    }
}
