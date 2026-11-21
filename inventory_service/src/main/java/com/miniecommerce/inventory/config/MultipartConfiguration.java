package com.miniecommerce.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/** Reject oversized uploads before they are written to disk. */
@Configuration
public class MultipartConfiguration implements WebFluxConfigurer {

    private final long maxBytes;

    public MultipartConfiguration(@Value("${app.phone-import.max-bytes:20971520}") long maxBytes) {
        this.maxBytes = maxBytes;
    }

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        DefaultPartHttpMessageReader partReader = new DefaultPartHttpMessageReader();
        partReader.setMaxParts(2);
        partReader.setMaxDiskUsagePerPart(maxBytes);
        partReader.setMaxInMemorySize(1024 * 1024);
        configurer.customCodecs().register(new MultipartHttpMessageReader(partReader));
    }
}
