package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class ResendPatientRelationshipEmailSender implements PatientRelationshipEmailSender {
    private final Resend resend;
    private final Executor executor;
    private final String from;
    private final String appUrl;

    public ResendPatientRelationshipEmailSender(
            Resend resend,
            @Qualifier("relationshipEmailExecutor") Executor executor,
            @Value("${relationships.mail.from}") String from,
            @Value("${relationships.mail.app-url}") String appUrl
    ) {
        this.resend = resend;
        this.executor = executor;
        this.from = from;
        this.appUrl = appUrl;
    }

    @Override
    public CompletableFuture<Void> sendInitialAccess(String name, String email, String nutritionist, String password) {
        return send(
                email,
                "Seu acesso ao Prato em Dia",
                "Olá, " + name + ".\n\n" + nutritionist
                        + " criou seu acesso ao Prato em Dia.\nE-mail: " + email
                        + "\nSenha inicial: " + password + "\n\nEntre em " + appUrl
                        + " e altere a senha no primeiro acesso."
        );
    }

    @Override
    public CompletableFuture<Void> sendRelationshipNotice(String name, String email, String nutritionist) {
        return send(
                email,
                "Novo vínculo no Prato em Dia",
                "Olá, " + name + ".\n\nSeu perfil foi vinculado ao nutricionista "
                        + nutritionist + ". Acesse " + appUrl + " com suas credenciais atuais."
        );
    }

    private CompletableFuture<Void> send(String to, String subject, String body) {
        var message = CreateEmailOptions.builder().from(from).to(to).subject(subject).text(body).build();
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        resend.emails().send(message);
                    } catch (ResendException e) {
                        throw new IllegalStateException("Resend rejected relationship email", e);
                    }
                },
                executor
        );
    }
}
