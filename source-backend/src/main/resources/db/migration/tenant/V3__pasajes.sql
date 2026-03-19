-- Tenant schema: pasajes module tables

CREATE TABLE IF NOT EXISTS seat_maps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID NOT NULL,
    travel_date DATE NOT NULL,
    seat_number INTEGER NOT NULL,
    floor_number INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    ticket_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP,
    UNIQUE(schedule_id, travel_date, seat_number, floor_number)
);

CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_code VARCHAR(50) NOT NULL UNIQUE,
    schedule_id UUID NOT NULL,
    customer_id UUID,
    seat_number INTEGER NOT NULL,
    floor_number INTEGER NOT NULL DEFAULT 1,
    travel_date DATE NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    sale_type VARCHAR(20) NOT NULL DEFAULT 'VENTANILLA',
    employee_id UUID,
    passenger_name VARCHAR(200),
    passenger_document VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reschedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_ticket_id UUID NOT NULL,
    new_ticket_id UUID,
    new_schedule_id UUID NOT NULL,
    reason VARCHAR(500),
    fee DECIMAL(10,2) DEFAULT 0,
    employee_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID NOT NULL UNIQUE,
    reason VARCHAR(500),
    retention_percent DECIMAL(5,2) DEFAULT 0,
    original_amount DECIMAL(10,2) NOT NULL,
    retained_amount DECIMAL(10,2),
    refunded_amount DECIMAL(10,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    employee_id UUID,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tickets_schedule_date ON tickets(schedule_id, travel_date);
CREATE INDEX IF NOT EXISTS idx_tickets_code ON tickets(ticket_code);
CREATE INDEX IF NOT EXISTS idx_seat_maps_schedule_date ON seat_maps(schedule_id, travel_date);
