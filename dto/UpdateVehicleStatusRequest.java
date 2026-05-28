package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVehicleStatusRequest(
        @NotBlank String vehicleStatus
) {
}

