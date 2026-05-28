package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public record CalculatePathRequest(
        @NotBlank String start,
        List<String> waypoints,
        @NotBlank String destination,
        LocalDateTime expectedDeliveryTime,
        Boolean force
) {
}
