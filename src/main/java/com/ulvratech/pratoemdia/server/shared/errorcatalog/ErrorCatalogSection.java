package com.ulvratech.pratoemdia.server.shared.errorcatalog;

import java.util.List;

/**
 * Contribuição de uma feature ao catálogo de erros. Cada feature tem a sua classe (um arquivo por
 * feature, sem conflito de merge) anotada com {@code @Component} e construtor sem argumentos.
 * O {@code ErrorCatalogTest} falha se um {@code CodedApplicationException} não estiver em nenhuma seção.
 */
public interface ErrorCatalogSection {
    List<ErrorCatalogEntry> entries();
}
