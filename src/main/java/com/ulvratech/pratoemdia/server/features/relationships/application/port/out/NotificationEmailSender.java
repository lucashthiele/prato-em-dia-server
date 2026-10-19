package com.ulvratech.pratoemdia.server.features.relationships.application.port.out;

import java.util.concurrent.CompletableFuture;

public interface NotificationEmailSender {
    CompletableFuture<Void> send(String to, String subject, String body);
}
