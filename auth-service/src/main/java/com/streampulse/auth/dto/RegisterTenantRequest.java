package com.streampulse.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
        @NotBlank @Size(max = 255) String tenantName,
        @NotBlank @Size(max = 100) String tenantSlug,
        @NotBlank @Email @Size(max = 255) String adminEmail,
        @NotBlank @Size(min = 8, max = 100) String adminPassword) {
}
