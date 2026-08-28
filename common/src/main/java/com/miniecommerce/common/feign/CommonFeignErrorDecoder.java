package com.miniecommerce.common.feign;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.response.ApiResponse;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatus;

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
                    HttpStatus.valueOf(response.status()),
                    apiResponse.getCode(),
                    apiResponse.getMessage()
            );
        } catch (IOException e) {
            return new AppException(
                    HttpStatus.valueOf(response.status()),
                    "DOWNSTREAM_ERROR",
                    "Call failed: " + methodKey
            );
        }
    }
}
