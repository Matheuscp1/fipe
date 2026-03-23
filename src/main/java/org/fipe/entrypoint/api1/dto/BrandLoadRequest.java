package org.fipe.entrypoint.api1.dto;

import jakarta.validation.constraints.NotBlank;

public record BrandLoadRequest(
        @NotBlank String code,
        @NotBlank String name
) {
}
