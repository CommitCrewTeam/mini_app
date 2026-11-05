package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import java.util.UUID;
import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("phone_draft")
public class PhoneDraftEntity {

    @Id
    private Long id;
    private UUID importId;
    private long rowNumber;
    private String key;
    private Json detail;
    private Integer quantity;
    private String status;
    private String errorMessage;
    private String updateKey;
    private Long matchedVersion;
}
