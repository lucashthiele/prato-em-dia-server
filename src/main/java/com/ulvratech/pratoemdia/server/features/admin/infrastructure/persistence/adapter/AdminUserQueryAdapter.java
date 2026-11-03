package com.ulvratech.pratoemdia.server.features.admin.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserFilter;
import com.ulvratech.pratoemdia.server.features.admin.application.dto.AdminUserView;
import com.ulvratech.pratoemdia.server.features.admin.application.port.out.AdminUserQuery;
import com.ulvratech.pratoemdia.server.features.admin.infrastructure.persistence.model.AdminUserModel;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageRequest;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminUserQueryAdapter implements AdminUserQuery {
    private static final char ESCAPE = '\\';

    private final AdminUserJpaRepository repository;

    public AdminUserQueryAdapter(AdminUserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<AdminUserView> search(AdminUserFilter filter, PageRequest page) {
        Specification<AdminUserModel> spec = (root, query, cb) -> cb.conjunction();
        if (filter.role() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("role"), filter.role()));
        }
        if (filter.status() != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), filter.status()));
        }
        var term = like(filter.query());
        if (term != null) {
            spec = spec.and(
                    (r, q, cb) -> cb.or(
                            cb.like(cb.lower(r.get("email")), term, ESCAPE),
                            cb.like(cb.lower(cb.coalesce(r.<String>get("fullName"), "")), term, ESCAPE)
                    )
            );
        }
        var result = repository.findAll(
                spec,
                org.springframework.data.domain.PageRequest.of(
                        page.page(), page.size(), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"))
                )
        );
        return new PageResult<>(
                result.getContent().stream().map(AdminUserQueryAdapter::toView).toList(), result.getNumber(),
                result.getSize(), result.getTotalElements(), result.getTotalPages()
        );
    }

    @Override
    public Optional<AdminUserView> findById(UUID id) {
        return repository.findById(id).map(AdminUserQueryAdapter::toView);
    }

    private static String like(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        var escaped = query.strip().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static AdminUserView toView(AdminUserModel m) {
        return new AdminUserView(
                m.getId(), m.getEmail(), m.getRole(), m.getStatus(), m.isPasswordChangeRequired(), m.getFullName(),
                m.getRegistrationNumber(), m.getCreatedAt(),
                m.getAccessEmailStatus() == null
                        ? null
                        : new AdminUserView.AccessEmail(
                                m.getAccessEmailStatus(),
                                m.getAccessEmailAttempts() == null ? 0 : m.getAccessEmailAttempts(),
                                m.getAccessEmailError()
                        )
        );
    }
}
