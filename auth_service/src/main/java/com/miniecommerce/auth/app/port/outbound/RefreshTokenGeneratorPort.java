package com.miniecommerce.auth.app.port.outbound;

public interface RefreshTokenGeneratorPort {
    String generate();
    String hash(String rawToken);
}
