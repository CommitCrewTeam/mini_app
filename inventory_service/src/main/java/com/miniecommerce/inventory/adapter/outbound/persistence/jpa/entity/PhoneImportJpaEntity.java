package com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity;

import com.miniecommerce.inventory.domain.enums.ImportMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;

@Entity
@Table(name = "phone_import")
public class PhoneImportJpaEntity {
    @Id
    public Long id;

    @Column(nullable = false)
    public String status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public ImportMode mode;

    @Version
    @Column(nullable = false)
    public Long version;

    public String error;

    @Column(name = "created_at", nullable = false)
    public OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public OffsetDateTime updatedAt;

    @Column(name = "upload_file_name")
    public String uploadFileName;
}
