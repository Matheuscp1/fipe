package org.fipe.integration.fipe.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record FipeBrand(
        @JsonAlias("codigo") String code,
        @JsonAlias("nome") String name
) {
}
