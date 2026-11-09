package com.miniecommerce.user.adapter.inbound.rest.mapper;

import com.miniecommerce.user.adapter.inbound.rest.dto.UserRequest;
import com.miniecommerce.user.adapter.inbound.rest.dto.UserResponse;
import com.miniecommerce.user.domain.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class UserRestMapper {

    public User toUser(UserRequest request) {
        return toUser(null, request);
    }

    public User toUser(UUID id, UserRequest request) {
        return new User(
                id,
                request.userId(),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.address(),
                request.birthDate());
    }

    public UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.id(),
                user.userId(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.address(),
                user.birthDate());
    }

    public List<UserResponse> toUserResponses(List<User> users) {
        return users.stream()
                .map(this::toUserResponse)
                .toList();
    }
}
