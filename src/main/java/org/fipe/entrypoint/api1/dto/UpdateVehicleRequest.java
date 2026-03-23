package org.fipe.entrypoint.api1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateVehicleRequest(
        @NotBlank @Size(max = 255) String modelName,
        @Size(max = 1000) String observations
) {
}
