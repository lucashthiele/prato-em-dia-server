package com.ulvratech.pratoemdia.server.features.relationships.application.dto;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;

public record PatientAccessEmail(RelationshipView view, String password) {
}
