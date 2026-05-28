package com.campusdelivery.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public record UpdateOrderRequest(
        @NotBlank String user,
        @NotBlank String userContact,
        @NotBlank String pickupCode,
        @NotBlank String destination,
        LocalDateTime expectedDeliveryTime,
        LocalDateTime orderTime,
        @NotBlank String orderNumber,
        @NotBlank String orderStatus,
        Double weightKg,
        Double lengthCm,
        Double widthCm,
        Double heightCm
) {
}
