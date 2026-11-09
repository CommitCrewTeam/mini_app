package com.miniecommerce.user.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * User aggregate of the user service.
 */
public record User(
        UUID id,
        String userId,
        String firstName,
        String lastName,
        String email,
        String address,
        LocalDate birthDate
) {

    public User {
        List<String> violations = new ArrayList<>();
        if (userId == null || userId.isBlank()) {
            violations.add("userId must not be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            violations.add("firstName must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            violations.add("lastName must not be blank");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            violations.add("email must be a valid email address");
        }
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid User: " + String.join("; ", violations));
        }
    }
}
