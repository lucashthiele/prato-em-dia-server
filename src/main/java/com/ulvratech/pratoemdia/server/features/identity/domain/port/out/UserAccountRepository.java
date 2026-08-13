package com.ulvratech.pratoemdia.server.features.identity.domain.port.out;

import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository {
    UserAccount save(UserAccount userAccount);

    Optional<UserAccount> findById(UUID userId);

    PageResult<UserAccount> findAll(PageRequest pageable);
    
    Optional<UserAccount> findByEmailIgnoreCase(String email);
}