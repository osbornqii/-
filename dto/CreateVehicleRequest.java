package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVehicleRequest(
        @NotBlank String vehicleNumber,
        @NotBlank String vehicleStatus,
        String history
) {
}

