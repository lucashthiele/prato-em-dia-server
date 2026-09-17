package com.ulvratech.pratoemdia.server.shared.pagination.domain;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
