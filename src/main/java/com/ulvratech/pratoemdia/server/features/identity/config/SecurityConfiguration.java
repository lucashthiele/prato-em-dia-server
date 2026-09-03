package com.ulvratech.pratoemdia.server.features.identity.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.*;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.security.*;
import com.ulvratech.pratoemdia.server.features.identity.presentation.exception.SecurityErrorResponseHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtConfigurationProperties.class)
public class SecurityConfiguration {

    private static final int BCRYPT_STRENGTH = 8;

    @Bean
    public PasswordHasher passwordHasher() {
        return new BcryptPasswordHasher(BCRYPT_STRENGTH);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RefreshTokenGenerator refreshTokenGenerator(
        Clock clock, JwtConfigurationProperties properties) {
        return new SecureRefreshTokenGenerator(new SecureRandom(), clock, properties);
    }

    @Bean
    public RefreshTokenHasher refreshTokenHasher() {
        return new Sha256RefreshTokenHasher();
    }

    @Bean
    public InitialPasswordGenerator initialPasswordGenerator() {
        return new SecureInitialPasswordGenerator(new SecureRandom());
    }

    @Bean
    public SecretKey jwtSecretKey(JwtConfigurationProperties properties) {
        return new SecretKeySpec(
            properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtConfigurationProperties properties) {
        var decoder =
            NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(
            new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.issuer())));
        return decoder;
    }

    @Bean
    public AccessTokenProvider accessTokenProvider(
        JwtEncoder jwtEncoder, JwtConfigurationProperties properties, Clock clock) {
        return new JwtAccessTokenProvider(jwtEncoder, properties, clock);
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        return new JwtAuthenticationConverter();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationConverter jwtAuthenticationConverter,
        SecurityErrorResponseHandler securityErrorResponseHandler,
        PasswordChangeRequiredFilter passwordChangeRequiredFilter)
        throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh")
                        .permitAll()
                        .requestMatchers("/api/v1/nutritionists/**")
                        .hasRole("NUTRITIONIST")
                        .requestMatchers("/api/v1/patients/**")
                        .hasRole("PATIENT")
                        .anyRequest()
                        .authenticated())
            .exceptionHandling(
                exceptions ->
                    exceptions
                        .authenticationEntryPoint(securityErrorResponseHandler)
                        .accessDeniedHandler(securityErrorResponseHandler))
            .oauth2ResourceServer(
                resourceServer ->
                    resourceServer
                        .authenticationEntryPoint(securityErrorResponseHandler)
                        .jwt(
                            jwt ->
                                jwt.jwtAuthenticationConverter(
                                    jwtAuthenticationConverter)))
            .addFilterAfter(passwordChangeRequiredFilter, BearerTokenAuthenticationFilter.class)
            .build();
    }
}
