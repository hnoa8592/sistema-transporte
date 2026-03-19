-- Tenant schema: finanzas module tables

CREATE TABLE IF NOT EXISTS cash_registers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    initial_amount DECIMAL(10,2) NOT NULL,
    final_amount DECIMAL(10,2),
    status VARCHAR(10) NOT NULL DEFAULT 'OPEN',
    notes VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cash_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cash_register_id UUID NOT NULL REFERENCES cash_registers(id),
    type VARCHAR(10) NOT NULL,
    concept VARCHAR(300),
    amount DECIMAL(10,2) NOT NULL,
    reference_id UUID,
    reference_type VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number VARCHAR(30) NOT NULL UNIQUE,
    customer_id UUID,
    customer_name VARCHAR(200),
    customer_document VARCHAR(20),
    subtotal DECIMAL(10,2) NOT NULL,
    tax_percent DECIMAL(5,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'EMITIDA',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoice_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL REFERENCES invoices(id),
    description VARCHAR(300) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS system_parameters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    param_key VARCHAR(100) NOT NULL UNIQUE,
    param_value VARCHAR(2000) NOT NULL,
    type VARCHAR(10) NOT NULL DEFAULT 'STRING',
    description VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    tenant_id VARCHAR(100),
    deleted_at TIMESTAMP
);

-- Default system parameters
INSERT INTO system_parameters (id, param_key, param_value, type, description, active) VALUES
    (gen_random_uuid(), 'RETENTION_PERCENT', '10', 'NUMBER', 'Porcentaje de retención para devoluciones', TRUE),
    (gen_random_uuid(), 'INVOICE_PREFIX', 'FAC', 'STRING', 'Prefijo para numeración de facturas', TRUE),
    (gen_random_uuid(), 'TAX_PERCENT', '13', 'NUMBER', 'Porcentaje de IVA', TRUE),
    (gen_random_uuid(), 'RESERVATION_EXPIRY_MINUTES', '30', 'NUMBER', 'Minutos para vencer una reserva', TRUE)
ON CONFLICT (param_key) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_cash_transactions_register ON cash_transactions(cash_register_id);
CREATE INDEX IF NOT EXISTS idx_invoices_number ON invoices(invoice_number);
CREATE INDEX IF NOT EXISTS idx_system_parameters_key ON system_parameters(param_key);
