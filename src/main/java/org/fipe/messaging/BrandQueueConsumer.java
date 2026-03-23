package org.fipe.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fipe.application.BrandLoadProcessor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BrandQueueConsumer {

    private final BrandLoadProcessor brandLoadProcessor;

    @RabbitListener(queues = "#{initialLoadQueue.name}")
    public void consume(BrandLoadMessage message, Message rawMessage) {
        log.info("Consumindo mensagem da fila: vehicleType={} code={} name={} rawBody={}",
                message.vehicleType(),
                message.code(),
                message.name(),
                new String(rawMessage.getBody()));
        brandLoadProcessor.process(message).block();
        log.info("Mensagem da marca {} consumida.", message.code());
    }
}
