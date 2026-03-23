package org.fipe.persistence;

import lombok.RequiredArgsConstructor;
import org.fipe.domain.BrandSummary;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
@RequiredArgsConstructor
public class VehicleSummaryRepository {

    private final DatabaseClient databaseClient;

    public Flux<BrandSummary> findDistinctBrands(String vehicleType, Integer reference) {
        return databaseClient.sql("""
                        SELECT DISTINCT brand_code, brand_name
                        FROM vehicles
                        WHERE vehicle_type = :vehicleType
                          AND reference_code = :reference
                        ORDER BY brand_name
                        """)
                .bind("vehicleType", vehicleType)
                .bind("reference", reference)
                .map((row, metadata) -> new BrandSummary(
                        row.get("brand_code", String.class),
                        row.get("brand_name", String.class)))
                .all();
    }
}
