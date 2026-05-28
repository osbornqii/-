package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateProfileRequest(
        @NotBlank String phone,
        String username,
        String oldPassword,
        String newPassword
) {
}

