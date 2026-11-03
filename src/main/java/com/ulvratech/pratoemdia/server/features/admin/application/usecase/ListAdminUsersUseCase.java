package com.ulvratech.pratoemdia.server.features.admin.application.usecase;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.ListAdminUsersInput;
import com.ulvratech.pratoemdia.server.features.admin.application.port.in.ListAdminUsers;
import com.ulvratech.pratoemdia.server.features.admin.application.port.out.AdminUserQuery;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ListAdminUsersUseCase implements ListAdminUsers {
    private final AdminUserQuery query;

    public ListAdminUsersUseCase(AdminUserQuery query) {
        this.query = query;
    }

    @Override
    public PageResult<AdminUserView> execute(ListAdminUsersInput input) {
        return query.search(input.filter(), new PageRequest(input.page(), input.size()));
    }
}
