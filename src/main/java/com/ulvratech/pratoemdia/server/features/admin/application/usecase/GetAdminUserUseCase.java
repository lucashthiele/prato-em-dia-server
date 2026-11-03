package com.ulvratech.pratoemdia.server.features.admin.application.usecase;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.GetAdminUser;
import com.ulvratech.pratoemdia.server.features.admin.application.port.out.AdminUserQuery;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminUserUseCase implements GetAdminUser {
    private final AdminUserQuery query;

    public GetAdminUserUseCase(AdminUserQuery query) {
        this.query = query;
    }

    @Override
    public AdminUserView execute(UUID id) {
        return query.findById(id).orElseThrow(UserAccountNotFoundException::new);
    }
}
