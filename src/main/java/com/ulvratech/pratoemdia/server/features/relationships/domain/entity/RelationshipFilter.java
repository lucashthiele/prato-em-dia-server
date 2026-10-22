package com.ulvratech.pratoemdia.server.features.relationships.domain.entity;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipSort;

/**
 * Optional criteria for listing a nutritionist's patients. {@code search} is already lowercased
 * and free of accents; {@code null} fields mean "no filter".
 */
public record RelationshipFilter(
        String search,
        RelationshipEmailStatus emailDeliveryStatus,
        boolean openDraftOnly,
        RelationshipSort sort) {
    public static RelationshipFilter none() {
        return new RelationshipFilter(null, null, false, RelationshipSort.CREATED_AT_DESC);
    }
}
