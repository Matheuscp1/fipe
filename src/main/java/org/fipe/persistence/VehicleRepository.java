package org.fipe.persistence;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface VehicleRepository extends ReactiveCrudRepository<VehicleEntity, UUID> {

    Flux<VehicleEntity> findAllByVehicleTypeAndReferenceCodeAndBrandCodeOrderByModelNameAsc(
            String vehicleType, Integer referenceCode, String brandCode);

    Mono<VehicleEntity> findByVehicleTypeAndReferenceCodeAndBrandCodeAndVehicleCode(
            String vehicleType, Integer referenceCode, String brandCode, String vehicleCode);
}
