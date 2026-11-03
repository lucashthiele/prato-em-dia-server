package com.ulvratech.pratoemdia.server.features.email.domain.enums;

public enum EmailType {
    /** Access email: stores only a reference; each attempt generates a new temporary password. */
    ACCESS, PLAN_GENERATED, PLAN_GENERATION_REJECTED
}
