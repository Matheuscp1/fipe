package org.fipe.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class VehicleUpsertRepository {

    private final DatabaseClient databaseClient;

    public Mono<VehicleEntity> upsert(VehicleEntity entity) {
        var spec = databaseClient.sql("""
                        INSERT INTO vehicles (id, vehicle_type, reference_code, brand_code, brand_name, vehicle_code, model_name, observations, created_at, updated_at)
                        VALUES (gen_random_uuid(), :vehicleType, :referenceCode, :brandCode, :brandName, :vehicleCode, :modelName, :observations, :createdAt, :updatedAt)
                        ON CONFLICT (vehicle_type, reference_code, brand_code, vehicle_code)
                        DO UPDATE SET
                            brand_name = EXCLUDED.brand_name,
                            model_name = EXCLUDED.model_name,
                            observations = COALESCE(vehicles.observations, EXCLUDED.observations),
                            updated_at = EXCLUDED.updated_at
                        RETURNING id, vehicle_type, reference_code, brand_code, brand_name, vehicle_code, model_name, observations, created_at, updated_at
                        """)
                .bind("vehicleType", entity.getVehicleType())
                .bind("referenceCode", entity.getReferenceCode())
                .bind("brandCode", entity.getBrandCode())
                .bind("brandName", entity.getBrandName())
                .bind("vehicleCode", entity.getVehicleCode())
                .bind("modelName", entity.getModelName())
                .bind("createdAt", entity.getCreatedAt())
                .bind("updatedAt", entity.getUpdatedAt());

        if (entity.getObservations() == null) {
            spec = spec.bindNull("observations", String.class);
        } else {
            spec = spec.bind("observations", entity.getObservations());
        }

        return spec.mapProperties(VehicleEntity.class).one();
    }
}
