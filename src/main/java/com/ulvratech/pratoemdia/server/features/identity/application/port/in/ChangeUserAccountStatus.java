package com.ulvratech.pratoemdia.server.features.identity.application.port.in;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangeUserAccountStatusInput;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

/** Changes the status and returns the previous one. Deactivating revokes every refresh token. */
public interface ChangeUserAccountStatus extends UseCase<ChangeUserAccountStatusInput, UserAccountStatus> {
}
