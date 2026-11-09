package com.miniecommerce.auth.app.result;

import java.time.LocalDate;
import java.util.UUID;

/**
 * User profile returned by the user service after registration.
 */
public record UserProfileResult(
        String firstName,
        String lastName,
        String email,
        String address,
        LocalDate birthDate
) {
}
