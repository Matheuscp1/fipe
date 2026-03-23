package org.fipe.entrypoint.api1.dto;

public record VehicleResponse(
        String vehicleType,
        Integer reference,
        String brandCode,
        String brandName,
        String code,
        String name,
        String observations
) {
}
