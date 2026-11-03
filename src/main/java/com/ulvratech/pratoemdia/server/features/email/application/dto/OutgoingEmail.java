package com.ulvratech.pratoemdia.server.features.email.application.dto;

public record OutgoingEmail(String to, String subject, String body) {
}
