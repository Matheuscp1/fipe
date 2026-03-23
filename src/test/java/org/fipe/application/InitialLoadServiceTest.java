package org.fipe.application;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.fipe.config.FipeProperties;
import org.fipe.integration.fipe.FipeGateway;
import org.fipe.integration.fipe.dto.FipeBrand;
import org.fipe.messaging.BrandQueuePublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class InitialLoadServiceTest {

    @Mock
    private FipeGateway fipeGateway;

    @Mock
    private BrandQueuePublisher brandQueuePublisher;

    private InitialLoadService initialLoadService;

    @BeforeEach
    void setUp() {
        FipeProperties properties = new FipeProperties(
                new FipeProperties.Security("admin", "admin123"),
                new FipeProperties.Messaging("queue"),
                new FipeProperties.Cache("brands", "vehicles:"),
                new FipeProperties.FipeApi("https://fipe.parallelum.com.br/api/v2", "cars", 278, null));
        initialLoadService = new InitialLoadService(properties, fipeGateway, brandQueuePublisher);
    }

    @Test
    void shouldUseDefaultVehicleTypeAndPublishBrandsFromFipeResponse() {
        when(fipeGateway.fetchBrands("cars", null)).thenReturn(Flux.just(
                new FipeBrand("21", "Fiat"),
                new FipeBrand("22", "Ford")));
        when(brandQueuePublisher.publish(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

        StepVerifier.create(initialLoadService.trigger(null))
                .assertNext(context -> {
                    org.junit.jupiter.api.Assertions.assertEquals("cars", context.vehicleType());
                    org.junit.jupiter.api.Assertions.assertEquals(2, context.totalBrandsEnqueued());
                })
                .verifyComplete();

        verify(brandQueuePublisher, times(2)).publish(org.mockito.ArgumentMatchers.any());
    }
}
