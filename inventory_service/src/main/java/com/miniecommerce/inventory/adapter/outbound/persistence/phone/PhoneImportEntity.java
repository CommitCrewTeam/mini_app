package com.miniecommerce.inventory.adapter.outbound.persistence.phone;

import com.miniecommerce.inventory.domain.ImportMode;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table("phone_import")
public class PhoneImportEntity {

    @Id
    private UUID id;
    private String status;
    private ImportMode mode;
    @Version
    private Long version;
    private String message;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
