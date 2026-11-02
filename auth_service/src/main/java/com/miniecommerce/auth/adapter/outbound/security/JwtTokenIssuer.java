package com.miniecommerce.auth.adapter.outbound.security;

import com.miniecommerce.auth.app.port.outbound.TokenIssuerPort;
import com.miniecommerce.auth.config.JwtProperties;
import com.miniecommerce.auth.domain.Auth;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuerPort {

    private static final String ISSUER = "auth_service";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public IssuedToken issueAccessToken(Auth auth) {
        Instant issuedAt = Instant.now();
        Duration ttl = jwtProperties.accessTokenTtl();
        Instant expiresAt = issuedAt.plus(ttl);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(auth.id().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("username", auth.username())
                .claim("active", auth.active())
                .claim("roles", auth.roles().stream().sorted().toList())
                .claim("permissions", auth.permissions().stream().sorted().toList())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new IssuedToken(token, ttl.toSeconds());
    }
}
