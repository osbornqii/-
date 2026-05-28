package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record UserRegisterRequest(
        @NotBlank String username,
        @NotBlank String phone,
        @NotBlank String password
) {
}

