package com.ulvratech.pratoemdia.server.features.identity.config;

import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.security.BcryptPasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfiguration {
    
    private final static int BCRYPT_STRENGTH = 8;
    
    @Bean
    public PasswordHasher passwordHasher() {
        return new BcryptPasswordHasher(BCRYPT_STRENGTH);
    }
}
