package com.miniecommerce.product.adapter.outbound.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("categories")
public class CategoryEntity {

    @Id
    private Long id;
    private String name;
    private Long parentId;
    private String slug;

    public CategoryEntity() {
    }

    public CategoryEntity(Long id, String name, Long parentId, String slug) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.slug = slug;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }
}
