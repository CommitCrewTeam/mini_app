package com.miniecommerce.user.app.port.inbound;

import com.miniecommerce.user.domain.User;

import java.util.List;

public interface ListUsersUseCase {

    List<User> listAll();
}
