package org.fipe.messaging;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BrandLoadMessage(
        String vehicleType,
        @JsonAlias("brandCode") String code,
        @JsonAlias("brandName") String name
) {
}
