package com.ulvratech.pratoemdia.server.shared.errorcatalog;

import com.ulvratech.pratoemdia.server.shared.application.exception.CodedApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guarda do catálogo de erros (LUC-50). Para estender: crie uma classe que implemente
 * {@link ErrorCatalogSection} na sua feature; o catálogo e este teste a descobrem sozinhos.
 */
class ErrorCatalogTest {
    private static final String BASE_PACKAGE = "com.ulvratech.pratoemdia.server";

    @Test
    void everyCodedApplicationExceptionHasAnEntryInTheCatalog() throws Exception {
        var catalog = discoveredCatalog();
        var missing = new ArrayList<String>();

        for (var type : exceptionsThatExposeACode()) {
            var code = codeOf(type);
            if (catalog.find(code).isEmpty()) {
                missing.add(type.getSimpleName() + " (" + code + ")");
            }
        }

        assertTrue(
                missing.isEmpty(),
                "Adicione ao ErrorCatalogSection da feature os códigos: " + missing
        );
    }

    @Test
    void catalogEntriesAreWellFormed() {
        var catalog = discoveredCatalog();
        var seen = new HashSet<String>();

        for (var entry : catalog.entries()) {
            assertTrue(seen.add(entry.code()), "código duplicado no catálogo: " + entry.code());
            assertTrue(entry.code().matches("[A-Z][A-Z0-9_]+"), "código fora do padrão: " + entry.code());
            assertFalse(entry.meaning().isBlank(), "significado vazio: " + entry.code());
            assertFalse(entry.routes().isEmpty(), "rotas vazias: " + entry.code());
            if (entry.kind() == ErrorCatalogEntry.Kind.ERROR) {
                assertNotNull(entry.status(), "erro sem status HTTP: " + entry.code());
                assertTrue(entry.status() >= 400 && entry.status() <= 599, "status inválido: " + entry.code());
            } else {
                assertEquals(null, entry.status(), "motivo não tem status HTTP: " + entry.code());
            }
        }
    }

    @Test
    void catalogIncludesTheCodesDecidedInPlanning() {
        var catalog = discoveredCatalog();

        assertEquals(426, catalog.find("CLIENT_OUTDATED").orElseThrow().status());
        assertTrue(catalog.find("CONSENT_REQUIRED").isPresent());
        assertEquals(
                ErrorCatalogEntry.Kind.REASON,
                catalog.find("NO_ALLOWED_FOODS").orElseThrow().kind()
        );
        assertEquals(409, catalog.find("PLAN_GENERATION_IN_PROGRESS").orElseThrow().status());
    }

    @Test
    void catalogCoversTheCodesEmittedByTheSharedHandlers() {
        var catalog = discoveredCatalog();

        for (var code : List.of(
                "VALIDATION_ERROR", "INVALID_REQUEST_BODY", "INVALID_REQUEST_PARAMETER", "ROUTE_NOT_FOUND",
                "METHOD_NOT_ALLOWED", "UNSUPPORTED_MEDIA_TYPE", "AUTHENTICATION_REQUIRED", "ACCESS_DENIED",
                "INTERNAL_ERROR", "TRACKING_NOT_FOUND", "TRACKING_FORBIDDEN", "TRACKING_CONFLICT",
                "TRACKING_INVALID", "WEEKLY_PLAN_NOT_FOUND", "PLANNING_ACCESS_DENIED", "PLANNING_CONFLICT",
                "INVALID_PLANNING", "INVALID_PRESCRIPTION", "CATALOG_INVALID_VALUE", "PROFILE_INVALID_VALUE",
                "RELATIONSHIP_INVALID_VALUE"
        )) {
            assertTrue(catalog.find(code).isPresent(), "código ausente do catálogo: " + code);
        }
    }

    private static ErrorCatalog discoveredCatalog() {
        try {
            var sections = new ArrayList<ErrorCatalogSection>();
            for (var type : discoverClasses(ErrorCatalogSection.class)) {
                sections.add((ErrorCatalogSection) type.getDeclaredConstructor().newInstance());
            }
            assertFalse(sections.isEmpty());
            return new ErrorCatalog(sections);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ErrorCatalogSection precisa de construtor sem argumentos", e);
        }
    }

    private static List<Class<?>> discoverClasses(Class<?> parent) {
        var scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                    org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition
            ) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(parent));
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(BASE_PACKAGE);
        return candidates.stream()
                .map(candidate -> {
                    try {
                        return Class.forName(candidate.getBeanClassName());
                    } catch (ClassNotFoundException e) {
                        throw new AssertionError(e);
                    }
                })
                .filter(type -> type != parent)
                .filter(type -> !Modifier.isAbstract(type.getModifiers()) && !type.isInterface())
                // Exceções criadas dentro de testes não fazem parte da API.
                .filter(type -> !type.getName().contains("Test"))
                .collect(Collectors.toList());
    }

    /** Subclasses de CodedApplicationException e exceções de application.exception com getCode(). */
    private static List<Class<?>> exceptionsThatExposeACode() {
        var types = new ArrayList<>(discoverClasses(CodedApplicationException.class));
        for (var type : discoverClasses(RuntimeException.class)) {
            boolean inApplicationExceptions = type.getPackageName().contains(".application.exception");
            boolean declaresGetCode = java.util.Arrays.stream(type.getDeclaredMethods())
                    .anyMatch(method -> method.getName().equals("getCode") && method.getParameterCount() == 0);
            if (inApplicationExceptions && declaresGetCode && !types.contains(type)) {
                types.add(type);
            }
        }
        return types;
    }

    private static String codeOf(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        var arguments = new Object[constructor.getParameterCount()];
        var parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = defaultValue(parameterTypes[i]);
        }
        var instance = constructor.newInstance(arguments);
        if (instance instanceof CodedApplicationException coded) {
            return coded.getCode();
        }
        return (String) type.getDeclaredMethod("getCode").invoke(instance);
    }

    private static Object defaultValue(Class<?> type) {
        if (type == String.class)
            return "x";
        if (type == UUID.class)
            return UUID.randomUUID();
        if (type == boolean.class)
            return false;
        if (type == int.class)
            return 0;
        if (type == long.class)
            return 0L;
        if (type.isEnum())
            return type.getEnumConstants()[0];
        return null;
    }
}
