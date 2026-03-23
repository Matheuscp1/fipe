package org.fipe.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.fipe.config.FipeProperties;
import org.fipe.integration.fipe.FipeGateway;
import org.fipe.integration.fipe.dto.FipeModel;
import org.fipe.messaging.BrandLoadMessage;
import org.fipe.persistence.VehicleEntity;
import org.fipe.persistence.VehicleUpsertRepository;
import org.fipe.support.VehicleCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BrandLoadProcessorTest {

    @Mock
    private FipeGateway fipeGateway;

    @Mock
    private VehicleUpsertRepository vehicleUpsertRepository;

    @Mock
    private VehicleCacheService vehicleCacheService;

    @Test
    void shouldFetchModelsPersistThemAndEvictCache() {
        FipeProperties properties = new FipeProperties(
                new FipeProperties.Security("admin", "admin123"),
                new FipeProperties.Messaging("queue"),
                new FipeProperties.Cache("brands", "vehicles:"),
                new FipeProperties.FipeApi("https://fipe.parallelum.com.br/api/v2", "cars", 278, null));
        BrandLoadProcessor processor = new BrandLoadProcessor(properties, fipeGateway, vehicleUpsertRepository, vehicleCacheService);
        BrandLoadMessage message = new BrandLoadMessage("cars", "21", "Fiat");

        when(fipeGateway.fetchModels("cars", "21", 278)).thenReturn(Flux.just(
                new FipeModel("100", "Argo"),
                new FipeModel("200", "Pulse")));
        when(vehicleUpsertRepository.upsert(any())).thenAnswer(invocation -> {
            VehicleEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return Mono.just(entity);
        });
        when(vehicleCacheService.evictBrand("cars", 278, "21")).thenReturn(Mono.just(1L));
        when(vehicleCacheService.evictBrands("cars", 278)).thenReturn(Mono.just(1L));

        StepVerifier.create(processor.process(message))
                .verifyComplete();

        verify(vehicleUpsertRepository, org.mockito.Mockito.times(2)).upsert(any());
        verify(vehicleCacheService).evictBrand("cars", 278, "21");
        verify(vehicleCacheService).evictBrands("cars", 278);
    }
}
