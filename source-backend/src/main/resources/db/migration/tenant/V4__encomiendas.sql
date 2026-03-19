-- Tenant schema: encomiendas module tables

CREATE TABLE IF NOT EXISTS parcels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tracking_code VARCHAR(50) NOT NULL UNIQUE,
    sender_id UUID,
    sender_name VARCHAR(200),
    sender_phone VARCHAR(20),
    recipient_id UUID,
    recipient_name VARCHAR(200),
    recipient_phone VARCHAR(20),
    schedule_id UUID,
    description VARCHAR(500),
    weight DECIMAL(10,3),
    declared_value DECIMAL(10,2),
    price DECIMAL(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'RECIBIDO',
    employee_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS parcel_trackings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parcel_id UUID NOT NULL REFERENCES parcels(id),
    status VARCHAR(20) NOT NULL,
    location VARCHAR(200),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_parcels_tracking_code ON parcels(tracking_code);
CREATE INDEX IF NOT EXISTS idx_parcels_status ON parcels(status);
CREATE INDEX IF NOT EXISTS idx_parcel_trackings_parcel_id ON parcel_trackings(parcel_id);
