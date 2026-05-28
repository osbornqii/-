package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVehicleRequest(
        @NotBlank String vehicleNumber,
        @NotBlank String vehicleStatus,
        String history
) {
}

