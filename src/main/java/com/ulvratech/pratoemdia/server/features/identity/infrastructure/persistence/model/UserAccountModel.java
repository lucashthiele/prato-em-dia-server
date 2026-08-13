package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelRole;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.enums.UserAccountModelStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_account")
@Builder
@Getter
@Setter
public class UserAccountModel {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "email")
    private String email;
    
    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserAccountModelRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private UserAccountModelStatus status;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @Column(name = "version")
    private BigInteger version;
    
    @PrePersist
    void prePersist() {
        version = BigInteger.valueOf(1);
    }
}
