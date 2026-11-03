package com.ulvratech.pratoemdia.server.features.admin.application.port.in;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.ListAdminUsersInput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

public interface ListAdminUsers extends UseCase<ListAdminUsersInput, PageResult<AdminUserView>> {
}
