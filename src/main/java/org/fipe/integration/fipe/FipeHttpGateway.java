package org.fipe.integration.fipe;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.fipe.config.FipeProperties;
import org.fipe.integration.fipe.dto.FipeBrand;
import org.fipe.integration.fipe.dto.FipeModel;
import org.fipe.integration.fipe.dto.FipeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class FipeHttpGateway implements FipeGateway {

    private final WebClient fipeWebClient;
    private final FipeProperties properties;

    @Override
    public Flux<FipeReference> fetchReferences() {
        String path = "/references";
        logRequest(path, null);
        return fipeWebClient.get()
                .uri(path)
                .headers(this::applySubscriptionToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> mapError(path, response.statusCode(), response.bodyToMono(String.class)))
                .bodyToFlux(FipeReference.class);
    }

    @Override
    public Flux<FipeBrand> fetchBrands(String vehicleType, Integer reference) {
        String path = "/" + vehicleType + "/brands";
        logRequest(path, reference);
        return fipeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{vehicleType}/brands")
                        .queryParams(buildReferenceQuery(reference))
                        .build(vehicleType))
                .headers(this::applySubscriptionToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> mapError(pathWithReference(path, reference), response.statusCode(), response.bodyToMono(String.class)))
                .bodyToFlux(FipeBrand.class);
    }

    @Override
    public Flux<FipeModel> fetchModels(String vehicleType, String brandCode, Integer reference) {
        String path = "/" + vehicleType + "/brands/" + brandCode + "/models";
        logRequest(path, reference);
        return fipeWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/{vehicleType}/brands/{brandCode}/models")
                        .build(vehicleType, brandCode))
                .headers(this::applySubscriptionToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> mapError(pathWithReference(path, reference), response.statusCode(), response.bodyToMono(String.class)))
                .bodyToFlux(FipeModel.class);
    }

    private MultiValueMap<String, String> buildReferenceQuery(Integer reference) {
        LinkedMultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        if (reference != null) {
            queryParams.add("reference", String.valueOf(reference));
        }
        return null;
    }

    private void applySubscriptionToken(HttpHeaders headers) {
        String token = properties.fipeApi().subscriptionToken();
        if (token != null && !token.isBlank()) {
            headers.add("X-Subscription-Token", token);
        }
        headers.add(HttpHeaders.ACCEPT, "application/json");
    }

    private void logRequest(String path, Integer reference) {
        String token = properties.fipeApi().subscriptionToken();
        log.info("Chamando FIPE v2: {}{} | tokenPresente={} | tokenPrefix={}",
                properties.fipeApi().baseUrl(),
                pathWithReference(path, reference),
                token != null && !token.isBlank(),
                maskToken(token));
    }

    private Mono<? extends Throwable> mapError(String target, HttpStatusCode statusCode, Mono<String> bodyMono) {
        return bodyMono.defaultIfEmpty("")
                .flatMap(body -> {
                    log.error("Erro ao chamar FIPE v2: {} | status={} | body={}", target, statusCode.value(), body);
                    return Mono.error(new IllegalStateException(
                            "FIPE retornou status " + statusCode.value() + " para " + target + ". Body: " + body));
                });
    }

    private String pathWithReference(String path, Integer reference) {
        return reference == null ? path : path + "?reference=" + reference;
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "absent";
        }
        int visible = Math.min(10, token.length());
        return token.substring(0, visible) + "...";
    }
}
