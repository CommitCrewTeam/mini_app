package com.miniecommerce.common.controller;

import com.miniecommerce.common.response.ApiResponse;

public abstract class BaseController {

    protected <T> ApiResponse<T> createSuccessResponse(T data) {
        return ApiResponse.success(data);
    }
}
