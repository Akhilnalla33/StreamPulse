package com.streampulse.auth.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static AuthException tenantSlugTaken(String slug) {
        return new AuthException(HttpStatus.CONFLICT, "Tenant slug already registered: " + slug);
    }

    public static AuthException emailTaken(String email) {
        return new AuthException(HttpStatus.CONFLICT, "Email already registered for this tenant: " + email);
    }

    public static AuthException invalidCredentials() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email, password, or tenant");
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "Refresh token is invalid, expired, or already used");
    }
}
