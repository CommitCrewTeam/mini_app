package com.miniecommerce.user.app.port.inbound;

import com.miniecommerce.user.domain.User;

import java.util.UUID;

public interface GetUserUseCase {

    User getUser(UUID id);
}
