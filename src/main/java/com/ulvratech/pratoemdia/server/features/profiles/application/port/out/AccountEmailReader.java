package com.ulvratech.pratoemdia.server.features.profiles.application.port.out;

import java.util.UUID;

/** E-mail de login da conta, que pertence à identidade e não ao perfil. */
public interface AccountEmailReader {
    String email(UUID userAccountId);
}
