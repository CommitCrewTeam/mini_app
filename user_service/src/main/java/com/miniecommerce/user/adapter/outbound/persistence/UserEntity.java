package com.miniecommerce.user.adapter.outbound.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Node("User")
public class UserEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    private String address;
    private LocalDate birthDate;
}
