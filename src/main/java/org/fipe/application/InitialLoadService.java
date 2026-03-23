package org.fipe.application;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.fipe.config.FipeProperties;
import org.fipe.integration.fipe.FipeGateway;
import org.fipe.messaging.BrandLoadMessage;
import org.fipe.messaging.BrandQueuePublisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitialLoadService {

    private final FipeProperties properties;
    private final FipeGateway fipeGateway;
    private final BrandQueuePublisher brandQueuePublisher;

    public Mono<InitialLoadContext> trigger(String requestedVehicleType) {
        String vehicleType = requestedVehicleType == null || requestedVehicleType.isBlank()
                ? properties.fipeApi().defaultVehicleType()
                : requestedVehicleType;

        log.info("InitialLoadService.trigger iniciado: vehicleType={}", vehicleType);

        return fipeGateway.fetchBrands(vehicleType, null)
                .flatMap(brand -> brandQueuePublisher.publish(new BrandLoadMessage(
                        vehicleType,
                        brand.code(),
                        brand.name()))
                        .thenReturn(brand))
                .count()
                .map(total -> total.intValue())
                .doOnSuccess(total -> log.info(
                        "InitialLoadService.trigger finalizado: vehicleType={} totalBrandsEnqueued={}",
                        vehicleType, total))
                .map(total -> new InitialLoadContext(vehicleType, total));
    }

    public record InitialLoadContext(String vehicleType, Integer totalBrandsEnqueued) {
    }
}
