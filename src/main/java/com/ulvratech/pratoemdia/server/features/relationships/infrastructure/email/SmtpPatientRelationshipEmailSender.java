package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.email;

import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.PatientRelationshipEmailSender;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpPatientRelationshipEmailSender implements PatientRelationshipEmailSender {
    private final JavaMailSender sender;
    private final String from;
    private final String appUrl;

    public SmtpPatientRelationshipEmailSender(
            JavaMailSender sender,
            @Value("${relationships.mail.from}") String from,
            @Value("${relationships.mail.app-url}") String appUrl
    ) {
        this.sender = sender;
        this.from = from;
        this.appUrl = appUrl;
    }

    public void sendInitialAccess(String name, String email, String nutritionist, String password) {
        send(
                email,
                "Seu acesso ao Prato em Dia",
                "Olá, "
                        + name
                        + ".\n\n"
                        + nutritionist
                        + " criou seu acesso ao Prato em Dia.\nE-mail: "
                        + email
                        + "\nSenha inicial: "
                        + password
                        + "\n\nEntre em "
                        + appUrl
                        + " e altere a senha no primeiro acesso."
        );
    }

    public void sendRelationshipNotice(String name, String email, String nutritionist) {
        send(
                email,
                "Novo vínculo no Prato em Dia",
                "Olá, "
                        + name
                        + ".\n\nSeu perfil foi vinculado ao nutricionista "
                        + nutritionist
                        + ". Acesse "
                        + appUrl
                        + " com suas credenciais atuais."
        );
    }

    private void send(String to, String subject, String text) {
        var message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        sender.send(message);
    }
}
