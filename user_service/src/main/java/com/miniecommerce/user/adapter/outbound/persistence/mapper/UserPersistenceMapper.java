package com.miniecommerce.user.adapter.outbound.persistence.mapper;

import com.miniecommerce.user.adapter.outbound.persistence.UserEntity;
import com.miniecommerce.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    public User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getBirthDate());
    }

    public UserEntity toEntity(User user) {
        return new UserEntity(
                user.id(),
                user.userId(),
                user.firstName(),
                user.lastName(),
                user.email(),
                user.address(),
                user.birthDate());
    }
}
