package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface RefreshAccessToken
    extends UseCase<RefreshAccessTokenInput, RefreshAccessTokenOutput> {}
