package com.minibanco.auth.user;

public record UserRequest(
        String username,
        String password
) {
}
