package com.miniecommerce.auth.app.command;

public record LoginCommand(String username, String rawPassword) {
}
