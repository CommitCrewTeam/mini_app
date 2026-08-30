package com.miniecommerce.common.feign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.common.response.ApiResponse;
import feign.Response;
import feign.codec.ErrorDecoder;

import java.io.IOException;
import java.io.InputStream;

public class CommonFeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;

    public CommonFeignErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        try (InputStream body = response.body().asInputStream()) {
            ApiResponse<?> apiResponse = objectMapper.readValue(body, new TypeReference<ApiResponse<?>>() {});
            return new AppException(
                    ErrorCode.from(apiResponse.getCode()),
                    apiResponse.getMessage()
            );
        } catch (IOException e) {
            return new AppException(
                    ErrorCode.BAD_GATEWAY,
                    "Call failed: " + methodKey
            );
        }
    }
}