package com.ulvratech.pratoemdia.server.features.admin.application.dto;

public record ListAdminUsersInput(AdminUserFilter filter, int page, int size) {
}
