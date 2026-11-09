package com.miniecommerce.auth.app.result;

import com.miniecommerce.auth.domain.Auth;

/**
 * Output of the register use case: the created account plus its user profile.
 */
public record RegisterResult(
        Auth auth,
        UserProfileResult user
) {
}
