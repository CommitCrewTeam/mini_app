package com.miniecommerce.product.domain;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

public class Category {

    private Long id;
    private String name;
    private Long parentId;
    private String slug;

    public Category() {
    }

    public Category(Long id, String name, Long parentId, String slug) {
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

    public boolean isRoot() {
        return parentId == null;
    }

    public void validate() {
        List<String> violations = new ArrayList<>();
        if (name == null || name.isBlank()) {
            violations.add("name must not be blank");
        }
        if (slug == null || slug.isBlank()) {
            violations.add("slug must not be blank");
        }
        if (!violations.isEmpty()) {
            throw new AppException(
                    ErrorCode.BAD_REQUEST,
                    "Invalid Category: " + String.join("; ", violations));
        }
    }
}
