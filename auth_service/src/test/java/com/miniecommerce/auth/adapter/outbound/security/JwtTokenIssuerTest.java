package com.miniecommerce.auth.adapter.outbound.security;

import com.miniecommerce.auth.app.port.outbound.TokenIssuerPort;
import com.miniecommerce.auth.config.JwtProperties;
import com.miniecommerce.auth.domain.Auth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenIssuerTest {

    private static final String SECRET = "mini-ecommerce-auth-dev-secret-key-change-me";
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private Jwt encodedJwt;

    private JwtTokenIssuer jwtTokenIssuer;

    @BeforeEach
    void setUp() {
        jwtTokenIssuer = new JwtTokenIssuer(
                jwtEncoder,
                new JwtProperties(SECRET, ACCESS_TOKEN_TTL)
        );
    }

    @Test
    void issuesAccessTokenWithExpectedClaims() {
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedJwt);
        when(encodedJwt.getTokenValue()).thenReturn("signed.jwt.token");

        Auth auth = new Auth(
                UUID.randomUUID(),
                "alice",
                "hash",
                true,
                Set.of("ROLE_CUSTOMER"),
                Set.of("product:read", "order:create")
        );
        Instant before = Instant.now();

        TokenIssuerPort.IssuedToken issuedToken = jwtTokenIssuer.issueAccessToken(auth);

        assertThat(issuedToken.value()).isEqualTo("signed.jwt.token");
        assertThat(issuedToken.expiresInSeconds()).isEqualTo(ACCESS_TOKEN_TTL.toSeconds());

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(parametersCaptor.capture());

        JwtEncoderParameters parameters = parametersCaptor.getValue();

        assertThat(parameters.getJwsHeader().getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
        assertThat(parameters.getJwsHeader().getType()).isEqualTo("JWT");

        JwtClaimsSet claims = parameters.getClaims();
        assertThat((String) claims.getClaim("iss")).isEqualTo("auth_service");
        assertThat(claims.getSubject()).isEqualTo(auth.id().toString());
        assertThat(claims.getId()).isNotNull();
        assertThat(UUID.fromString(claims.getId())).isNotNull();
        assertThat(claims.getIssuedAt()).isAfterOrEqualTo(before);
        assertThat(claims.getExpiresAt()).isEqualTo(claims.getIssuedAt().plus(ACCESS_TOKEN_TTL));
        assertThat((String) claims.getClaim("username")).isEqualTo("alice");
        assertThat((Boolean) claims.getClaim("active")).isTrue();
        assertThat(claims.getClaimAsStringList("roles")).containsExactly("ROLE_CUSTOMER");
        assertThat(claims.getClaimAsStringList("permissions"))
                .containsExactly("order:create", "product:read");
    }

    @Test
    void generatesUniqueTokenIdForEachIssuedToken() {
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedJwt);
        when(encodedJwt.getTokenValue()).thenReturn("signed.jwt.token");

        Auth auth = new Auth(UUID.randomUUID(), "alice", "hash", true);

        jwtTokenIssuer.issueAccessToken(auth);
        jwtTokenIssuer.issueAccessToken(auth);

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor =
                ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder, times(2)).encode(parametersCaptor.capture());

        List<String> tokenIds = parametersCaptor.getAllValues().stream()
                .map(parameters -> parameters.getClaims().getId())
                .toList();

        assertThat(tokenIds).doesNotHaveDuplicates();
        assertThat(tokenIds).allSatisfy(tokenId -> assertThat(UUID.fromString(tokenId)).isNotNull());
    }
}
