package org.fipe.application;

import java.time.Instant;
import org.fipe.config.FipeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fipe.integration.fipe.FipeGateway;
import org.fipe.messaging.BrandLoadMessage;
import org.fipe.persistence.VehicleEntity;
import org.fipe.persistence.VehicleUpsertRepository;
import org.fipe.support.VehicleCacheService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandLoadProcessor {

    private final FipeProperties properties;
    private final FipeGateway fipeGateway;
    private final VehicleUpsertRepository vehicleUpsertRepository;
    private final VehicleCacheService vehicleCacheService;

    public Mono<Void> process(BrandLoadMessage message) {
        Integer reference = properties.fipeApi().defaultReference();
        return fipeGateway.fetchModels(message.vehicleType(), message.code(), reference)
                .filter(model -> {
                    boolean valid = StringUtils.hasText(model.code()) && StringUtils.hasText(model.name());
                    if (!valid) {
                        log.warn("Modelo ignorado por payload invalido: vehicleType={} brandCode={} rawCode={} rawName={}",
                                message.vehicleType(), message.code(), model.code(), model.name());
                    }
                    return valid;
                })
                .map(model -> VehicleEntity.builder()
                        .vehicleType(message.vehicleType())
                        .referenceCode(reference)
                        .brandCode(message.code())
                        .brandName(message.name())
                        .vehicleCode(model.code())
                        .modelName(model.name())
                        .observations(null)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .flatMap(vehicleUpsertRepository::upsert)
                .then(vehicleCacheService.evictBrand(message.vehicleType(), reference, message.code()))
                .then(vehicleCacheService.evictBrands(message.vehicleType(), reference))
                .then()
                .doOnSuccess(ignored -> log.info("Marca {} processada com sucesso.", message.code()))
                .doOnError(error -> log.error("Falha ao processar marca {}", message.code(), error));
    }
}
