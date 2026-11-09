package com.miniecommerce.gateway.application;

import com.miniecommerce.gateway.domain.model.AuthenticationResult;
import com.miniecommerce.gateway.domain.model.TokenStatus;
import com.miniecommerce.gateway.domain.model.TokenVerification;
import com.miniecommerce.gateway.domain.port.out.TokenVerificationPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenAuthenticationServiceTest {

    private final TokenVerificationPort tokenVerificationPort = mock(TokenVerificationPort.class);

    private final TokenAuthenticationService service =
            new TokenAuthenticationService(tokenVerificationPort);

    @Test
    void returnsValidWhenTokenIsVerified() {
        when(tokenVerificationPort.verify("token")).thenReturn(Mono.just(new TokenVerification(true)));

        StepVerifier.create(service.authenticate("token"))
                .expectNext(AuthenticationResult.of(TokenStatus.VALID))
                .verifyComplete();
    }

    @Test
    void returnsInvalidWhenTokenIsRejected() {
        when(tokenVerificationPort.verify("token"))
                .thenReturn(Mono.just(TokenVerification.invalid()));

        StepVerifier.create(service.authenticate("token"))
                .expectNext(AuthenticationResult.of(TokenStatus.INVALID))
                .verifyComplete();
    }

    @Test
    void returnsInvalidWhenPortReturnsEmpty() {
        when(tokenVerificationPort.verify("token")).thenReturn(Mono.empty());

        StepVerifier.create(service.authenticate("token"))
                .expectNext(AuthenticationResult.of(TokenStatus.INVALID))
                .verifyComplete();
    }

    @Test
    void returnsUnavailableWhenPortFails() {
        when(tokenVerificationPort.verify("token"))
                .thenReturn(Mono.error(new IllegalStateException("auth service down")));

        StepVerifier.create(service.authenticate("token"))
                .expectNext(AuthenticationResult.of(TokenStatus.UNAVAILABLE))
                .verifyComplete();
    }
}
