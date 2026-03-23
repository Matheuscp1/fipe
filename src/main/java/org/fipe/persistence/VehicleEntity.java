package org.fipe.persistence;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("vehicles")
public class VehicleEntity {

    @Id
    private UUID id;

    @Column("vehicle_type")
    private String vehicleType;

    @Column("reference_code")
    private Integer referenceCode;

    @Column("brand_code")
    private String brandCode;

    @Column("brand_name")
    private String brandName;

    @Column("vehicle_code")
    private String vehicleCode;

    @Column("model_name")
    private String modelName;

    @Column("observations")
    private String observations;

    @Column("created_at")
    private Instant createdAt;

    @Column("updated_at")
    private Instant updatedAt;
}
