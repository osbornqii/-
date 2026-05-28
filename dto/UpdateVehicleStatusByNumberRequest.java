package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVehicleStatusByNumberRequest(
        @NotBlank String vehicleNumber,
        @NotBlank String vehicleStatus
) {
}

