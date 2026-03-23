package org.fipe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class WebClientConfiguration {

    @Bean
    WebClient fipeWebClient(FipeProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.fipeApi().baseUrl())
                .filter(logRequestHeaders())
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }

    private ExchangeFilterFunction logRequestHeaders() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            String token = request.headers().getFirst("X-Subscription-Token");
            log.info("WebClient request => method={} url={} header.X-Subscription-Token.present={} header.X-Subscription-Token.prefix={} accept={}",
                    request.method(),
                    request.url(),
                    token != null && !token.isBlank(),
                    maskToken(token),
                    request.headers().getFirst(HttpHeaders.ACCEPT));
            return Mono.just(request);
        });
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "absent";
        }
        int visible = Math.min(10, token.length());
        return token.substring(0, visible) + "...";
    }
}
