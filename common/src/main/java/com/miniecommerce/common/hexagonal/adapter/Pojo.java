package com.miniecommerce.common.hexagonal.adapter;

import java.nio.file.Path;

public class Pojo {

    /** Thông tin generic về file upload đã được lưu xuống disk. */
    public record StagedFile(String filename, long size, Path path) {}
}
