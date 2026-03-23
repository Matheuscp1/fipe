package org.fipe.messaging;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.fipe.config.FipeProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrandQueuePublisher {

    private final RabbitTemplate rabbitTemplate;
    private final FipeProperties properties;

    public Mono<Void> publish(BrandLoadMessage message) {
        return Mono.fromRunnable(() -> {
                    log.info("Publicando mensagem na fila: queue={} vehicleType={} code={} name={}",
                            properties.messaging().initialLoadQueue(),
                            message.vehicleType(),
                            message.code(),
                            message.name());
                    rabbitTemplate.convertAndSend(properties.messaging().initialLoadQueue(), message);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
