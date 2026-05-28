package com.campusdelivery.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUpdateProfileRequest(
        @NotNull Long id,
        String username,
        String oldPassword,
        String newPassword
) {
}

