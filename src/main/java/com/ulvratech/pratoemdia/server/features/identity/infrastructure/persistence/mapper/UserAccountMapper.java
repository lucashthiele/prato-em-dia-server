package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelRole;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelStatus;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.UserAccountModel;
import org.springframework.stereotype.Component;

@Component
public class UserAccountMapper {

    public UserAccount toEntity(UserAccountModel userAccountModel) {
        return new UserAccount(
            userAccountModel.getId(),
            userAccountModel.getEmail(),
            userAccountModel.getPasswordHash(),
            UserAccountRole.valueOf(userAccountModel.getRole().name()),
            UserAccountStatus.valueOf(userAccountModel.getStatus().name())
        );
    }
    
    public UserAccountModel toModel(UserAccount userAccount) {
        return UserAccountModel.builder()
            .id(userAccount.getId())
            .email(userAccount.getEmail())
            .passwordHash(userAccount.getPasswordHash())
            .role(UserAccountModelRole.valueOf(userAccount.getRole().name()))
            .status(UserAccountModelStatus.valueOf(userAccount.getStatus().name()))
            .build();
    }
}
