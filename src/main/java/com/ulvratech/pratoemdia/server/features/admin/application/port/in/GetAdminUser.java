package com.ulvratech.pratoemdia.server.features.admin.application.port.in;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

import java.util.UUID;

public interface GetAdminUser extends UseCase<UUID, AdminUserView> {
}
