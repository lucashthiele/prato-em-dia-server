package com.ulvratech.pratoemdia.server.features.admin.application.port.out;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserFilter;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserQuery {
    PageResult<AdminUserView> search(AdminUserFilter filter, PageRequest page);

    Optional<AdminUserView> findById(UUID id);
}
