package com.ulvratech.pratoemdia.server.features.relationships.domain.enums;

import java.util.Arrays;

public enum RelationshipSort {
    CREATED_AT_DESC("createdAt,desc"), CREATED_AT_ASC("createdAt,asc"), NAME_ASC("fullName,asc");

    private final String value;

    RelationshipSort(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static RelationshipSort parse(String value) {
        if (value == null || value.isBlank())
            return CREATED_AT_DESC;
        return Arrays.stream(values())
                .filter(s -> s.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported sort: " + value));
    }
}
