package com.ulvratech.pratoemdia.server.shared.errorcatalog;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Catálogo único de códigos de erro, montado a partir de todas as {@link ErrorCatalogSection}. */
@Component
public class ErrorCatalog {
    private final List<ErrorCatalogEntry> entries;

    public ErrorCatalog(List<ErrorCatalogSection> sections) {
        this.entries = sections.stream()
                .flatMap(section -> section.entries().stream())
                .sorted(Comparator.comparing(ErrorCatalogEntry::code))
                .toList();
    }

    public List<ErrorCatalogEntry> entries() {
        return entries;
    }

    public Optional<ErrorCatalogEntry> find(String code) {
        return entries.stream().filter(entry -> entry.code().equals(code)).findFirst();
    }
}
