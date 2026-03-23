package org.fipe.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fipe.config.FipeProperties;
import org.fipe.domain.BrandSummary;
import org.fipe.domain.VehicleDetails;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleCacheService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FipeProperties properties;

    public Flux<BrandSummary> getBrands(String vehicleType, Integer reference) {
        String key = buildBrandsKey(vehicleType, reference);
        return redisTemplate.opsForValue().get(key)
                .flatMapMany(value -> Flux.fromIterable(read(value, new TypeReference<List<BrandSummary>>() {
                })))
                .onErrorResume(error -> Flux.empty());
    }

    public Mono<Boolean> putBrands(String vehicleType, Integer reference, List<BrandSummary> brands) {
        return redisTemplate.opsForValue()
                .set(buildBrandsKey(vehicleType, reference), write(brands), TTL)
                .onErrorReturn(false);
    }

    public Flux<VehicleDetails> getVehiclesByBrand(String vehicleType, Integer reference, String brandCode) {
        String key = buildVehiclesKey(vehicleType, reference, brandCode);
        return redisTemplate.opsForValue().get(key)
                .flatMapMany(value -> Flux.fromIterable(read(value, new TypeReference<List<VehicleDetails>>() {
                })))
                .onErrorResume(error -> Flux.empty());
    }

    public Mono<Boolean> putVehicles(String vehicleType, Integer reference, String brandCode, List<VehicleDetails> vehicles) {
        String key = buildVehiclesKey(vehicleType, reference, brandCode);
        return redisTemplate.opsForValue().set(key, write(vehicles), TTL)
                .onErrorReturn(false);
    }

    public Mono<Long> evictBrand(String vehicleType, Integer reference, String brandCode) {
        return redisTemplate.delete(buildVehiclesKey(vehicleType, reference, brandCode))
                .onErrorReturn(0L);
    }

    public Mono<Long> evictBrands(String vehicleType, Integer reference) {
        return redisTemplate.delete(buildBrandsKey(vehicleType, reference))
                .onErrorReturn(0L);
    }

    private String buildBrandsKey(String vehicleType, Integer reference) {
        return properties.cache().brandsKeyPrefix() + ":" + vehicleType + ":" + reference;
    }

    private String buildVehiclesKey(String vehicleType, Integer reference, String brandCode) {
        return properties.cache().vehiclesKeyPrefix() + vehicleType + ":" + reference + ":" + brandCode;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao serializar cache.", exception);
        }
    }

    private <T> T read(String value, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(value, typeReference);
        } catch (Exception exception) {
            log.warn("Falha ao desserializar cache. Conteudo sera ignorado.", exception);
            throw new IllegalStateException("Falha ao desserializar cache.", exception);
        }
    }
}
