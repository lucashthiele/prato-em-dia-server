package com.ulvratech.pratoemdia.server.features.identity.application.port.out;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;

public interface AccessTokenProvider {
    GeneratedAccessToken generate(UserAccount userAccount);
}
