package com.miniecommerce.auth.config;

import com.miniecommerce.auth.adapter.outbound.persistence.AuthEntity;
import com.miniecommerce.auth.adapter.outbound.persistence.AuthJpaRepository;
import com.miniecommerce.auth.adapter.outbound.persistence.RoleEntity;
import com.miniecommerce.auth.adapter.outbound.persistence.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final AuthJpaRepository authRepository;
    private final RoleJpaRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SuperAdminProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (authRepository.existsByUsername(properties.getUsername())) {
            return;
        }

        RoleEntity superAdminRole = roleRepository.findByCode(ROLE_SUPER_ADMIN)
                .orElseThrow(() -> new IllegalStateException("Required role ROLE_SUPER_ADMIN does not exist"));

        AuthEntity superAdmin = authRepository.save(AuthEntity.builder()
                .username(properties.getUsername())
                .passwordHash(passwordEncoder.encode(properties.getPassword()))
                .active(true)
                .build());

        superAdminRole.getUsers().add(superAdmin);
        superAdmin.getRoles().add(superAdminRole);
    }
}
