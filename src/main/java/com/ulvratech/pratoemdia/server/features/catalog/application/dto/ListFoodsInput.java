package com.ulvratech.pratoemdia.server.features.catalog.application.dto;

public record ListFoodsInput(int page, int size, String q) {
    public ListFoodsInput(int page, int size) {
        this(page, size, null);
    }
}
