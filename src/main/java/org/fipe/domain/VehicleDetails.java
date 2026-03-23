package org.fipe.domain;

public record VehicleDetails(
        String vehicleType,
        Integer reference,
        String brandCode,
        String brandName,
        String vehicleCode,
        String modelName,
        String observations
) {
}
