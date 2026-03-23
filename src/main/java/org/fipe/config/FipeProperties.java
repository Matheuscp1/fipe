package org.fipe.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "application")
public record FipeProperties(
        Security security,
        Messaging messaging,
        Cache cache,
        FipeApi fipeApi
) {

    public record Security(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record Messaging(
            @NotBlank String initialLoadQueue
    ) {
    }

    public record Cache(
            @NotBlank String brandsKeyPrefix,
            @NotBlank String vehiclesKeyPrefix
    ) {
    }

    public record FipeApi(
            @NotBlank String baseUrl,
            @NotBlank String defaultVehicleType,
            Integer defaultReference,
            String subscriptionToken
    ) {
    }
}
