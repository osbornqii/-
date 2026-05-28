package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAdminRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String userType
) {
}

