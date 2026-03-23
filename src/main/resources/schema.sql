CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_type VARCHAR(30) NOT NULL DEFAULT 'cars',
    reference_code INTEGER NOT NULL DEFAULT 278,
    brand_code VARCHAR(50) NOT NULL,
    brand_name VARCHAR(255) NOT NULL,
    vehicle_code VARCHAR(50) NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    observations VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS vehicle_type VARCHAR(30);
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS reference_code INTEGER;
ALTER TABLE vehicles ADD COLUMN IF NOT EXISTS observations VARCHAR(1000);

UPDATE vehicles SET vehicle_type = 'cars' WHERE vehicle_type IS NULL;
UPDATE vehicles SET reference_code = 278 WHERE reference_code IS NULL;

ALTER TABLE vehicles ALTER COLUMN vehicle_type SET DEFAULT 'cars';
ALTER TABLE vehicles ALTER COLUMN reference_code SET DEFAULT 278;
ALTER TABLE vehicles ALTER COLUMN vehicle_type SET NOT NULL;
ALTER TABLE vehicles ALTER COLUMN reference_code SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_vehicles_brand_code ON vehicles (brand_code);
CREATE INDEX IF NOT EXISTS idx_vehicles_brand_name ON vehicles (brand_name);
CREATE INDEX IF NOT EXISTS idx_vehicles_vehicle_type_reference ON vehicles (vehicle_type, reference_code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_brand_ref_model
    ON vehicles (vehicle_type, reference_code, brand_code, vehicle_code);
