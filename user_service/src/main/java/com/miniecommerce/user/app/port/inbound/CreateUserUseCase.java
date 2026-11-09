package com.miniecommerce.user.app.port.inbound;

import com.miniecommerce.user.domain.User;

public interface CreateUserUseCase {

    User createUser(User user);
}
