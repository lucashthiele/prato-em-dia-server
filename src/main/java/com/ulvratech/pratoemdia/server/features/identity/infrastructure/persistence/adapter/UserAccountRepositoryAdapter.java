package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.UserAccountModel;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UserAccountRepositoryAdapter implements UserAccountRepository {
    private final UserAccountMapper mapper;
    private final UserAccountJpaRepository repository;

    public UserAccountRepositoryAdapter(
            UserAccountMapper mapper, UserAccountJpaRepository repository
    ) {
        this.mapper = mapper;
        this.repository = repository;
    }

    @Override
    public UserAccount save(UserAccount userAccount) {
        var userAccountModel = userAccount.getId() == null
                ? mapper.toModel(userAccount)
                : repository
                        .findById(userAccount.getId())
                        .map(
                                existing -> {
                                    existing.setEmail(userAccount.getEmail());
                                    existing.setPasswordHash(userAccount.getPasswordHash());
                                    existing.setRole(
                                            com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelRole
                                                    .valueOf(
                                                            userAccount.getRole().name()
                                                    )
                                    );
                                    existing.setStatus(
                                            com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelStatus
                                                    .valueOf(
                                                            userAccount.getStatus().name()
                                                    )
                                    );
                                    existing.setPasswordChangeRequired(
                                            userAccount.isPasswordChangeRequired()
                                    );
                                    return existing;
                                }
                        )
                        .orElseGet(() -> mapper.toModel(userAccount));
        var persistedUserAccountModel = repository.save(userAccountModel);
        return mapper.toEntity(persistedUserAccountModel);
    }

    @Override
    public Optional<UserAccount> findById(UUID userId) {
        return repository.findById(userId).map(mapper::toEntity);
    }

    @Override
    public PageResult<UserAccount> findAll(PageRequest pageRequest) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                pageRequest.page(), pageRequest.size()
        );

        Page<UserAccountModel> result = repository.findAll(pageable);

        return new PageResult<>(
                result.getContent().stream().map(mapper::toEntity).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Override
    public Optional<UserAccount> findByEmailIgnoreCase(String email) {
        return repository.findByEmail(email.toLowerCase()).map(mapper::toEntity);
    }
}
