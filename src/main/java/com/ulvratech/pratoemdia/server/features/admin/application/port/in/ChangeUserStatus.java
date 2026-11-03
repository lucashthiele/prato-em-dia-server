package com.ulvratech.pratoemdia.server.features.admin.application.port.in;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.ChangeUserStatusInput;

public interface ChangeUserStatus {
    void execute(ChangeUserStatusInput input);
}
