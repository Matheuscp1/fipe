package org.fipe.application;

import lombok.RequiredArgsConstructor;
import org.fipe.config.FipeProperties;
import org.fipe.domain.BrandSummary;
import org.fipe.domain.VehicleDetails;
import org.fipe.persistence.VehicleEntity;
import org.fipe.persistence.VehicleRepository;
import org.fipe.persistence.VehicleSummaryRepository;
import org.fipe.support.VehicleCacheService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class VehicleQueryService {

    private final FipeProperties properties;
    private final VehicleRepository vehicleRepository;
    private final VehicleSummaryRepository vehicleSummaryRepository;
    private final VehicleCacheService vehicleCacheService;

    public Flux<BrandSummary> findBrands(String vehicleType) {
        Integer resolvedReference = resolveReference();

        return vehicleCacheService.getBrands(vehicleType, resolvedReference)
                .switchIfEmpty(Flux.defer(() -> vehicleSummaryRepository.findDistinctBrands(vehicleType, resolvedReference)
                        .collectList()
                        .flatMapMany(brands -> vehicleCacheService.putBrands(vehicleType, resolvedReference, brands)
                                .thenMany(Flux.fromIterable(brands)))));
    }

    public Flux<VehicleDetails> findVehiclesByBrand(String vehicleType, String brandCode) {
        Integer resolvedReference = resolveReference();

        return vehicleCacheService.getVehiclesByBrand(vehicleType, resolvedReference, brandCode)
                .switchIfEmpty(Flux.defer(() -> vehicleRepository.findAllByVehicleTypeAndReferenceCodeAndBrandCodeOrderByModelNameAsc(
                                vehicleType, resolvedReference, brandCode)
                        .map(VehicleQueryService::toDomain)
                        .collectList()
                        .flatMapMany(vehicles -> vehicleCacheService.putVehicles(vehicleType, resolvedReference, brandCode, vehicles)
                                .thenMany(Flux.fromIterable(vehicles)))));
    }

    public Mono<VehicleDetails> findVehicleById(String vehicleType, String brandCode, String vehicleCode) {
        Integer resolvedReference = resolveReference();

        return vehicleRepository.findByVehicleTypeAndReferenceCodeAndBrandCodeAndVehicleCode(
                        vehicleType, resolvedReference, brandCode, vehicleCode)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Veiculo nao encontrado para a marca informada.")))
                .map(VehicleQueryService::toDomain);
    }

    public Mono<VehicleDetails> updateVehicle(
            String vehicleType,
            String brandCode,
            String vehicleCode,
            String modelName,
            String observations) {
        Integer resolvedReference = resolveReference();

        return vehicleRepository.findByVehicleTypeAndReferenceCodeAndBrandCodeAndVehicleCode(
                        vehicleType, resolvedReference, brandCode, vehicleCode)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Veiculo nao encontrado para a marca informada.")))
                .flatMap(entity -> {
                    entity.setModelName(modelName);
                    entity.setObservations(observations);
                    return vehicleRepository.save(entity);
                })
                .flatMap(saved -> vehicleCacheService.evictBrand(
                        saved.getVehicleType(), saved.getReferenceCode(), saved.getBrandCode()).thenReturn(saved))
                .flatMap(saved -> vehicleCacheService.evictBrands(
                        saved.getVehicleType(), saved.getReferenceCode()).thenReturn(saved))
                .map(VehicleQueryService::toDomain);
    }

    public static VehicleDetails toDomain(VehicleEntity entity) {
        return new VehicleDetails(
                entity.getVehicleType(),
                entity.getReferenceCode(),
                entity.getBrandCode(),
                entity.getBrandName(),
                entity.getVehicleCode(),
                entity.getModelName(),
                entity.getObservations());
    }

    private Integer resolveReference() {
        return properties.fipeApi().defaultReference();
    }
}
