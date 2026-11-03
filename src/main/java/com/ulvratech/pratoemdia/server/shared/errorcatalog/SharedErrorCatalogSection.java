package com.ulvratech.pratoemdia.server.shared.errorcatalog;

import java.util.List;
import org.springframework.stereotype.Component;

import static com.ulvratech.pratoemdia.server.shared.errorcatalog.ErrorCatalogEntry.error;

/** Códigos transversais: validação, segurança, compatibilidade de cliente e falhas inesperadas. */
@Component
public class SharedErrorCatalogSection implements ErrorCatalogSection {
    @Override
    public List<ErrorCatalogEntry> entries() {
        return List.of(
                error(
                        "VALIDATION_ERROR", 400,
                        "O corpo ou os parâmetros violam as regras de validação. O detalhe por campo vem em errors[].",
                        "*"
                ),
                error("INVALID_REQUEST_BODY", 400, "O corpo da requisição está ausente ou não é um JSON válido.", "*"),
                error(
                        "INVALID_REQUEST_PARAMETER", 400,
                        "Parâmetro de rota ou de consulta ausente, ou com tipo ou formato inválido.", "*"
                ),
                error("ROUTE_NOT_FOUND", 404, "A rota solicitada não existe.", "*"),
                error("METHOD_NOT_ALLOWED", 405, "O método HTTP não é aceito nesta rota.", "*"),
                error("UNSUPPORTED_MEDIA_TYPE", 415, "O Content-Type enviado não é suportado.", "*"),
                error(
                        "AUTHENTICATION_REQUIRED", 401, "Sessão ausente, inválida ou expirada. Autentique-se de novo.",
                        "*"
                ),
                error("ACCESS_DENIED", 403, "O papel da conta não permite esta operação.", "*"),
                error(
                        "PASSWORD_CHANGE_REQUIRED", 403,
                        "A conta usa a senha inicial e precisa trocá-la antes de usar o restante da API.", "*"
                ),
                error(
                        "CONSENT_REQUIRED", 403,
                        "A versão vigente dos termos e da política de privacidade ainda não foi aceita. "
                                + "Só a rota de aceite e o logout ficam liberados.",
                        "*"
                ),
                error(
                        "CLIENT_OUTDATED", 426,
                        "A versão do cliente (cabeçalho X-Client-Version) está abaixo do mínimo da plataforma. "
                                + "O cliente deve pedir ao usuário que atualize o app ou recarregue o portal.",
                        "*"
                ),
                error(
                        "INTERNAL_ERROR", 500,
                        "Falha inesperada. A mensagem é genérica e nenhum detalhe interno é exposto.", "*"
                )
        );
    }
}
