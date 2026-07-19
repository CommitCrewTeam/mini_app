package com.miniecommerce.product;

import com.miniecommerce.common.domain.ProductDomain;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProductServiceApplication {

    public static void main(String[] args) {
        ProductDomain productDomain = null;
        SpringApplication.run(ProductServiceApplication.class, args);
    }
}
