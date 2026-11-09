package com.miniecommerce.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Bảo vệ user_service bằng JWT do auth_service phát hành.
 * <p>
 * Service tự verify chữ ký/hạn của token (không phụ thuộc kết quả từ gateway),
 * sau đó dùng roles/permissions trong token cho {@code @PreAuthorize}.
 * Secret phải trùng với {@code security.jwt.secret} của auth_service.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/internal/users"
    };

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight không mang token.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${security.jwt.secret}") String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return Stream.of("roles", "permissions")
                .flatMap(claim -> claimValues(jwt, claim).stream())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<String> claimValues(Jwt jwt, String claim) {
        List<String> values = jwt.getClaimAsStringList(claim);
        return values == null ? List.of() : values;
    }
}
