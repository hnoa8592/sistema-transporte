-- =============================================
-- V3: Módulo SIAT Bolivia - Facturación Computarizada en Línea
-- =============================================

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
CREATE INDEX IF NOT EXISTS idx_invoices_number ON invoices(invoice_number);

-- Configuración SIAT por sucursal/punto de venta
CREATE TABLE siat_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nit VARCHAR(20) NOT NULL,
    razon_social VARCHAR(200) NOT NULL,
    codigo_sistema VARCHAR(50) NOT NULL,
    codigo_actividad VARCHAR(10) NOT NULL,
    codigo_sucursal INTEGER NOT NULL DEFAULT 0,
    codigo_punto_venta INTEGER,
    direccion VARCHAR(300),
    municipio VARCHAR(100),
    telefono VARCHAR(30),
    codigo_ambiente INTEGER NOT NULL DEFAULT 2,
    codigo_modalidad INTEGER NOT NULL DEFAULT 2,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    UNIQUE (nit, codigo_sucursal, codigo_punto_venta, tenant_id)
);

-- CUIS (Código Único de Inicio de Sistema)
CREATE TABLE siat_cuis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    siat_config_id UUID NOT NULL REFERENCES siat_config(id),
    cuis VARCHAR(100) NOT NULL,
    fecha_vigencia TIMESTAMP NOT NULL,
    codigo_sucursal INTEGER NOT NULL DEFAULT 0,
    codigo_punto_venta INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_siat_cuis_config ON siat_cuis(siat_config_id, activo);

-- CUFD (Código Único de Facturación Diaria)
CREATE TABLE siat_cufd (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    siat_config_id UUID NOT NULL REFERENCES siat_config(id),
    cufd VARCHAR(200) NOT NULL,
    codigo_control VARCHAR(50) NOT NULL,
    codigo_para_qr VARCHAR(300),
    fecha_vigencia TIMESTAMP NOT NULL,
    codigo_sucursal INTEGER NOT NULL DEFAULT 0,
    codigo_punto_venta INTEGER,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_siat_cufd_config ON siat_cufd(siat_config_id, activo);
CREATE INDEX idx_siat_cufd_vigencia ON siat_cufd(fecha_vigencia);

-- Catálogos SIAT (tipo y items)
CREATE TABLE siat_catalogo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_catalogo VARCHAR(60) NOT NULL,
    codigo VARCHAR(20) NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    vigente BOOLEAN NOT NULL DEFAULT TRUE,
    datos_extra JSONB,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP,
    UNIQUE (tipo_catalogo, codigo, tenant_id)
);

CREATE INDEX idx_siat_catalogo_tipo ON siat_catalogo(tipo_catalogo, vigente);

-- Facturas SIAT (vinculadas a invoices existentes)
CREATE TABLE siat_factura (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID REFERENCES invoices(id),
    siat_config_id UUID NOT NULL REFERENCES siat_config(id),
    siat_cufd_id UUID NOT NULL REFERENCES siat_cufd(id),
    cuf VARCHAR(100) UNIQUE,
    numero_factura BIGINT NOT NULL,
    codigo_sucursal INTEGER NOT NULL DEFAULT 0,
    codigo_punto_venta INTEGER,
    fecha_emision TIMESTAMP NOT NULL,
    -- Datos del emisor (snapshot)
    nit_emisor VARCHAR(20) NOT NULL,
    razon_social_emisor VARCHAR(200),
    -- Datos del receptor
    nombre_razon_social VARCHAR(200),
    codigo_tipo_documento_identidad INTEGER NOT NULL DEFAULT 1,
    numero_documento VARCHAR(30) NOT NULL,
    complemento VARCHAR(10),
    codigo_cliente VARCHAR(50),
    -- Importes
    importe_total_sujeto_iva DECIMAL(16,2) NOT NULL DEFAULT 0,
    importe_total DECIMAL(16,2) NOT NULL,
    tipo_cambio DECIMAL(10,4) NOT NULL DEFAULT 1.0000,
    codigo_moneda INTEGER NOT NULL DEFAULT 1,
    -- Método de pago
    codigo_metodo_pago INTEGER NOT NULL DEFAULT 1,
    -- Clasificación
    codigo_actividad VARCHAR(10),
    codigo_documento_sector INTEGER NOT NULL DEFAULT 1,
    -- Estado SIAT
    estado_emision VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    tipo_emision INTEGER NOT NULL DEFAULT 1,
    codigo_recepcion VARCHAR(100),
    mensaje_siat VARCHAR(500),
    -- XML e imágenes
    xml_content TEXT,
    xml_firmado TEXT,
    qr_content TEXT,
    pdf_path VARCHAR(500),
    -- Para paquetes
    siat_paquete_id UUID,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_siat_factura_invoice ON siat_factura(invoice_id);
CREATE INDEX idx_siat_factura_estado ON siat_factura(estado_emision);
CREATE INDEX idx_siat_factura_cuf ON siat_factura(cuf);
CREATE INDEX idx_siat_factura_numero ON siat_factura(numero_factura);

-- Detalle de facturas SIAT
CREATE TABLE siat_factura_detalle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    siat_factura_id UUID NOT NULL REFERENCES siat_factura(id) ON DELETE CASCADE,
    numero_linea INTEGER NOT NULL,
    actividad_economica VARCHAR(10),
    codigo_producto_sin INTEGER NOT NULL DEFAULT 84111,
    codigo_producto VARCHAR(20),
    descripcion VARCHAR(500) NOT NULL,
    cantidad DECIMAL(16,4) NOT NULL,
    unidad_medida INTEGER NOT NULL DEFAULT 58,
    precio_unitario DECIMAL(16,2) NOT NULL,
    monto_descuento DECIMAL(16,2) NOT NULL DEFAULT 0,
    sub_total DECIMAL(16,2) NOT NULL,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_siat_factura_detalle_factura ON siat_factura_detalle(siat_factura_id);

-- Paquetes de emisión SIAT
CREATE TABLE siat_paquete (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    siat_config_id UUID NOT NULL REFERENCES siat_config(id),
    siat_cufd_id UUID NOT NULL REFERENCES siat_cufd(id),
    codigo_sucursal INTEGER NOT NULL DEFAULT 0,
    codigo_punto_venta INTEGER,
    cantidad_facturas INTEGER NOT NULL DEFAULT 0,
    tipo_emision INTEGER NOT NULL DEFAULT 2,
    codigo_recepcion VARCHAR(100),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    estado_validacion VARCHAR(30),
    mensaje_siat VARCHAR(500),
    archivo_zip TEXT,
    fecha_emision TIMESTAMP NOT NULL,
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP
);

-- Eventos Significativos SIAT
CREATE TABLE siat_evento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    siat_config_id UUID NOT NULL REFERENCES siat_config(id),
    siat_cufd_id UUID NOT NULL REFERENCES siat_cufd(id),
    codigo_evento INTEGER NOT NULL,
    descripcion VARCHAR(500),
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP,
    codigo_sucursal INTEGER NOT NULL DEFAULT 0,
    codigo_punto_venta INTEGER,
    codigo_recepcion VARCHAR(100),
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    mensaje_siat VARCHAR(500),
    tenant_id VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    deleted_at TIMESTAMP
);

CREATE INDEX idx_siat_evento_config ON siat_evento(siat_config_id);

-- Agregar FK de siat_factura a siat_paquete (ahora que la tabla existe)
ALTER TABLE siat_factura ADD CONSTRAINT fk_siat_factura_paquete
    FOREIGN KEY (siat_paquete_id) REFERENCES siat_paquete(id);

-- Agregar columnas SIAT a tabla invoices existente
ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS siat_factura_id UUID REFERENCES siat_factura(id),
    ADD COLUMN IF NOT EXISTS emitida_siat BOOLEAN NOT NULL DEFAULT FALSE;
