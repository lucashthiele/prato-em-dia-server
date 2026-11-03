package com.ulvratech.pratoemdia.server.features.email.application.service;

import com.ulvratech.pratoemdia.server.features.email.application.dto.OutgoingEmail;
import com.ulvratech.pratoemdia.server.features.email.domain.entity.OutboxEmail;

/** Builds the access email at send time, because the password only exists during the attempt. */
public final class AccessEmailTemplate {
    static final String SUBJECT = "Seu acesso ao Prato em Dia";

    private AccessEmailTemplate() {
    }

    public static OutgoingEmail build(OutboxEmail email, String temporaryPassword, String appUrl) {
        var greeting = email.getRecipientName() == null || email.getRecipientName().isBlank()
                ? "Olá."
                : "Olá, " + email.getRecipientName() + ".";
        var inviter = email.getInviterName() == null ? "A administração" : email.getInviterName();
        var body = greeting + "\n\n" + inviter + " criou seu acesso ao Prato em Dia.\nE-mail: "
                + email.getRecipient() + "\nSenha temporária: " + temporaryPassword
                + "\n\nEsta senha substitui qualquer senha temporária enviada anteriormente: use somente esta."
                + "\nEntre em " + appUrl + " e altere a senha no primeiro acesso.";
        return new OutgoingEmail(email.getRecipient(), SUBJECT, body);
    }
}
