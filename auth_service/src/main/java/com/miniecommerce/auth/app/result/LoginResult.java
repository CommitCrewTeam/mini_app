package com.miniecommerce.auth.app.result;

import com.miniecommerce.auth.domain.Auth;

/**
 * Output of the login use case: the authenticated account plus the issued access token.
 */
public record LoginResult(
        Auth auth,
        String accessToken,
        String refreshToken
) {
}
