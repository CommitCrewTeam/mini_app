package com.miniecommerce.gateway.adapter.out.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Cấu hình hạ tầng HTTP cho adapter đầu ra.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient authServiceWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${AUTH_SERVICE_URL:http://localhost:8091}") String authServiceUrl
    ) {
        return webClientBuilder
                .baseUrl(authServiceUrl)
                .build();
    }

    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return new CorsWebFilter(source);
    }

    @Bean
    AuthHttpClient authHttpClient(WebClient authServiceWebClient) {
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(authServiceWebClient))
                .build();

        return httpServiceProxyFactory.createClient(AuthHttpClient.class);
    }
}
