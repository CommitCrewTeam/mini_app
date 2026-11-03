package com.miniecommerce.auth.app.port.outbound;

import com.miniecommerce.auth.domain.Auth;

public interface TokenIssuerPort {

    IssuedToken issueAccessToken(Auth auth);

    record IssuedToken(String value, long expiresInSeconds) {
    }
}
