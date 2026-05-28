package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record SubmitOrderRequest(
        @NotBlank String user,
        @NotBlank String userContact,
        @NotBlank String pickupCode,
        @NotBlank String destination,
        LocalDateTime expectedDeliveryTime
) {
}

