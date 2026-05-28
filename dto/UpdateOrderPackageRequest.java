package com.campusdelivery.dto;

public record UpdateOrderPackageRequest(
        Double weightKg,
        Double lengthCm,
        Double widthCm,
        Double heightCm
) {
}

