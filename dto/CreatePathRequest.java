package com.campusdelivery.dto;

import java.time.LocalDateTime;

public record CreatePathRequest(
        LocalDateTime computedAt,
        LocalDateTime expectedDeliveryTime,
        String planningResult
) {
}

