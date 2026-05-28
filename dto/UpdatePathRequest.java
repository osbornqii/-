package com.campusdelivery.dto;

import java.time.LocalDateTime;

public record UpdatePathRequest(
        LocalDateTime computedAt,
        LocalDateTime expectedDeliveryTime,
        String planningResult
) {
}

