package com.miniecommerce.order.adapter.outbound.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniecommerce.common.feign.CommonFeignErrorDecoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new CommonFeignErrorDecoder(objectMapper);
    }
}
