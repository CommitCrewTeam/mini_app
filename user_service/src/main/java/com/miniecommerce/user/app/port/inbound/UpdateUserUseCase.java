package com.miniecommerce.user.app.port.inbound;

import com.miniecommerce.user.domain.User;

import java.util.UUID;

public interface UpdateUserUseCase {

    User updateUser(UUID id, User user);
}
