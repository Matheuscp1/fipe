package org.fipe.entrypoint.api1.dto;

public record InitialLoadResponse(
        String message,
        String vehicleType,
        Integer totalBrandsEnqueued
) {
}
