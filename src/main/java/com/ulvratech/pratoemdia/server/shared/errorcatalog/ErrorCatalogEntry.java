package com.ulvratech.pratoemdia.server.shared.errorcatalog;

import java.util.List;

/**
 * Uma linha do catálogo de erros: o {@code code} que o cliente recebe no envelope
 * {@code {status, code, message, errors[]}}, o status HTTP, o significado e as rotas onde aparece.
 * {@link Kind#REASON} descreve valores de campos como {@code blockedReason}, que não são erros HTTP.
 */
public record ErrorCatalogEntry(String code, Integer status, String meaning, List<String> routes, Kind kind) {
    public enum Kind {
        ERROR, REASON
    }

    public static ErrorCatalogEntry error(String code, int status, String meaning, String... routes) {
        return new ErrorCatalogEntry(code, status, meaning, List.of(routes), Kind.ERROR);
    }

    public static ErrorCatalogEntry reason(String code, String meaning, String... routes) {
        return new ErrorCatalogEntry(code, null, meaning, List.of(routes), Kind.REASON);
    }
}
