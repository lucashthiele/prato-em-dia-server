package com.ulvratech.pratoemdia.server.features.identity.config;

import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.security.BcryptPasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    
    private final static int BCRYPT_STRENGTH = 8;
    
    @Bean
    public PasswordHasher passwordHasher() {
        return new BcryptPasswordHasher(BCRYPT_STRENGTH);
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }
}
