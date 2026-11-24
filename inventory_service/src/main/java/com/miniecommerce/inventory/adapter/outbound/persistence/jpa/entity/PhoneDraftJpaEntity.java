package com.miniecommerce.inventory.adapter.outbound.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "phone_draft")
public class PhoneDraftJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Version @Column(nullable = false) public Long version;
    @Column(name = "import_id", nullable = false) public Long importId;
    @Column(name = "row_number", nullable = false) public long rowNumber;
    public String key;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") public Map<String, Object> detail;
    public Integer quantity;
    @Column(nullable = false) public String status;
    @Column(name = "error_message") public String errorMessage;
    @Column(name = "matched_version") public Long matchedVersion;
}
