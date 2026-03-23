package org.fipe.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.fipe.config.FipeProperties;
import org.fipe.domain.BrandSummary;
import org.fipe.persistence.VehicleEntity;
import org.fipe.persistence.VehicleRepository;
import org.fipe.persistence.VehicleSummaryRepository;
import org.fipe.support.VehicleCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class VehicleQueryServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleSummaryRepository vehicleSummaryRepository;

    @Mock
    private VehicleCacheService vehicleCacheService;

    private VehicleQueryService buildService() {
        FipeProperties properties = new FipeProperties(
                new FipeProperties.Security("admin", "admin123"),
                new FipeProperties.Messaging("queue"),
                new FipeProperties.Cache("brands", "vehicles:"),
                new FipeProperties.FipeApi("https://fipe.parallelum.com.br/api/v2", "cars", 278, null));
        return new VehicleQueryService(properties, vehicleRepository, vehicleSummaryRepository, vehicleCacheService);
    }

    @Test
    void shouldReturnBrandsFromCacheWhenAvailable() {
        VehicleQueryService service = buildService();
        when(vehicleCacheService.getBrands("cars", 278)).thenReturn(Flux.just(new BrandSummary("21", "Fiat")));

        StepVerifier.create(service.findBrands("cars"))
                .expectNext(new BrandSummary("21", "Fiat"))
                .verifyComplete();

        verify(vehicleSummaryRepository, never()).findDistinctBrands("cars", 278);
    }

    @Test
    void shouldUpdateVehicleAndEvictRelatedCache() {
        VehicleQueryService service = buildService();
        VehicleEntity entity = VehicleEntity.builder()
                .id(UUID.randomUUID())
                .vehicleType("cars")
                .referenceCode(278)
                .brandCode("21")
                .brandName("Fiat")
                .vehicleCode("100")
                .modelName("Argo")
                .observations("old")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(vehicleRepository.findByVehicleTypeAndReferenceCodeAndBrandCodeAndVehicleCode("cars", 278, "21", "100"))
                .thenReturn(Mono.just(entity));
        when(vehicleRepository.save(any(VehicleEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(vehicleCacheService.evictBrand("cars", 278, "21")).thenReturn(Mono.just(1L));
        when(vehicleCacheService.evictBrands("cars", 278)).thenReturn(Mono.just(1L));

        StepVerifier.create(service.updateVehicle("cars", "21", "100", "Argo Trekking", "ajustado"))
                .assertNext(vehicle -> {
                    org.junit.jupiter.api.Assertions.assertEquals("Argo Trekking", vehicle.modelName());
                    org.junit.jupiter.api.Assertions.assertEquals("ajustado", vehicle.observations());
                })
                .verifyComplete();
    }
}
