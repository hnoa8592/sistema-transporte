-- =============================================================================
-- seed_test_operacion.sql
-- Datos de prueba: fleets, buses, drivers, routes, schedules
--
-- USO MANUAL (no es migración Flyway):
--   psql -U postgres -d transporte \
--        -v schema=demo \
--        -f scripts/seed_test_operacion.sql
--
-- O bien dentro de psql:
--   \set schema demo
--   \i scripts/seed_test_operacion.sql
--
-- ADVERTENCIA: Solo para entornos de desarrollo/testing.
--              NO ejecutar en producción.
-- =============================================================================

\set ON_ERROR_STOP on

-- Cambiar al esquema del tenant de prueba (ajustar si es necesario)
SET search_path TO transporte_dev;

DO $$
BEGIN
    RAISE NOTICE '=== Iniciando seed_test_operacion en esquema: % ===', current_schema();
END $$;

-- -----------------------------------------------------------------------------
-- 1. FLOTA
-- -----------------------------------------------------------------------------
INSERT INTO fleets (id, name, description, active, created_by, tenant_id)
VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Flota Principal',   'Buses interurbanos de largo recorrido', TRUE, 'seed', current_schema()),
    ('a1000000-0000-0000-0000-000000000002', 'Flota Cama',        'Buses cama premium con butacas 180°',   TRUE, 'seed', current_schema()),
    ('a1000000-0000-0000-0000-000000000003', 'Flota Semi-Cama',   'Servicio semi-cama económico',          TRUE, 'seed', current_schema())
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 2. DEPARTAMENTOS DE BOLIVIA
-- -----------------------------------------------------------------------------
INSERT INTO departments (id, name, code, active, created_by, tenant_id)
VALUES
    ('b1000000-0000-0000-0000-000000000001', 'La Paz',       'LPZ', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000002', 'Cochabamba',   'CBA', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000003', 'Santa Cruz',   'SCZ', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000004', 'Oruro',        'ORU', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000005', 'Potosí',       'PTS', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000006', 'Sucre',        'CHQ', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000007', 'Beni',        'BNI', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000008', 'Tarija',        'TJA', TRUE, 'seed', current_schema()),
    ('b1000000-0000-0000-0000-000000000009', 'Pando',        'PND', TRUE, 'seed', current_schema())
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 3. PROVINCIAS
-- -----------------------------------------------------------------------------
INSERT INTO provinces (id, name, department_id, active, created_by, tenant_id)
VALUES
    ('c1000000-0000-0000-0000-000000000001', 'Murillo',       'b1000000-0000-0000-0000-000000000001', TRUE, 'seed', current_schema()),
    ('c1000000-0000-0000-0000-000000000002', 'Cercado',       'b1000000-0000-0000-0000-000000000002', TRUE, 'seed', current_schema()),
    ('c1000000-0000-0000-0000-000000000003', 'Andrés Ibáñez', 'b1000000-0000-0000-0000-000000000003', TRUE, 'seed', current_schema()),
    ('c1000000-0000-0000-0000-000000000004', 'Cercado-OR',    'b1000000-0000-0000-0000-000000000004', TRUE, 'seed', current_schema()),
    ('c1000000-0000-0000-0000-000000000005', 'Tomás Frías',   'b1000000-0000-0000-0000-000000000005', TRUE, 'seed', current_schema()),
    ('c1000000-0000-0000-0000-000000000006', 'Oropeza',       'b1000000-0000-0000-0000-000000000006', TRUE, 'seed', current_schema())
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 4. UBICACIONES / TERMINALES DE ORIGEN Y DESTINO
-- -----------------------------------------------------------------------------
INSERT INTO locations (id, name, province_id, latitude, longitude, active, created_by, tenant_id)
VALUES
    ('d1000000-0000-0000-0000-000000000001', 'La Paz - Terminal de Buses',     'c1000000-0000-0000-0000-000000000001', -16.4897, -68.1193, TRUE, 'seed', current_schema()),
    ('d1000000-0000-0000-0000-000000000002', 'Cochabamba - Terminal de Buses', 'c1000000-0000-0000-0000-000000000002', -17.3895, -66.1568, TRUE, 'seed', current_schema()),
    ('d1000000-0000-0000-0000-000000000003', 'Santa Cruz - Terminal Bimodal',  'c1000000-0000-0000-0000-000000000003', -17.7892, -63.1975, TRUE, 'seed', current_schema()),
    ('d1000000-0000-0000-0000-000000000004', 'Oruro - Terminal de Buses',      'c1000000-0000-0000-0000-000000000004', -17.9840, -67.1068, TRUE, 'seed', current_schema()),
    ('d1000000-0000-0000-0000-000000000005', 'Potosí - Terminal de Buses',     'c1000000-0000-0000-0000-000000000005', -19.5836, -65.7531, TRUE, 'seed', current_schema()),
    ('d1000000-0000-0000-0000-000000000006', 'Sucre - Terminal de Buses',      'c1000000-0000-0000-0000-000000000006', -19.0477, -65.2591, TRUE, 'seed', current_schema())
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 5. BUSES
--    Pisos 1: 40 asientos | Doble piso: 25 + 25 = 50
-- -----------------------------------------------------------------------------
INSERT INTO buses (id, plate, model, brand, manufacture_year, fleet_id,
                   has_two_floors, total_seats, seats_first_floor, seats_second_floor,
                   active, notes, created_by, tenant_id)
VALUES
    -- Flota Principal (piso único)
    ('e1000000-0000-0000-0000-000000000001', '2340-ABC', 'Marcopolo G7 1200', 'Volvo',      2021,
     'a1000000-0000-0000-0000-000000000001', FALSE, 40, 40, NULL,
     TRUE, 'Servicio estándar ruta La Paz-Cochabamba', 'seed', current_schema()),

    ('e1000000-0000-0000-0000-000000000002', '1180-DEF', 'Paradiso 1200',     'Mercedes',   2020,
     'a1000000-0000-0000-0000-000000000001', FALSE, 40, 40, NULL,
     TRUE, 'Servicio estándar ruta La Paz-Oruro', 'seed', current_schema()),

    ('e1000000-0000-0000-0000-000000000003', '5521-GHI', 'Comil Campione',    'Scania',     2019,
     'a1000000-0000-0000-0000-000000000001', FALSE, 40, 40, NULL,
     TRUE, 'Servicio estándar ruta Cochabamba-Santa Cruz', 'seed', current_schema()),

    -- Flota Cama (doble piso)
    ('e1000000-0000-0000-0000-000000000004', '3370-JKL', 'Marcopolo Paradiso', 'Volvo',     2023,
     'a1000000-0000-0000-0000-000000000002', TRUE,  50, 25, 25,
     TRUE, 'Bus cama doble piso, butacas 180°', 'seed', current_schema()),

    ('e1000000-0000-0000-0000-000000000005', '8812-MNO', 'Comil Invictus 1200','Mercedes',  2022,
     'a1000000-0000-0000-0000-000000000002', TRUE,  50, 25, 25,
     TRUE, 'Bus cama doble piso, wifi, outlet USB', 'seed', current_schema()),

    -- Flota Semi-Cama (piso único)
    ('e1000000-0000-0000-0000-000000000006', '7743-PQR', 'Busscar Vissta Buss','Volvo',     2018,
     'a1000000-0000-0000-0000-000000000003', FALSE, 44, 44, NULL,
     TRUE, 'Semi-cama económico', 'seed', current_schema()),

    ('e1000000-0000-0000-0000-000000000007', '4490-STU', 'Marcopolo Viaggio',  'Scania',    2020,
     'a1000000-0000-0000-0000-000000000003', FALSE, 44, 44, NULL,
     TRUE, 'Semi-cama con TV individual', 'seed', current_schema())
ON CONFLICT (id) DO NOTHING;

-- Manejo de placa UNIQUE separado (por si las filas ya existen con otro id)
-- Los ON CONFLICT (id) DO NOTHING ya son suficientes para re-ejecución limpia.

-- -----------------------------------------------------------------------------
-- 6. CHOFERES
-- -----------------------------------------------------------------------------
INSERT INTO drivers (id, dni, first_name, last_name, license_number, license_category,
                     license_expiry_date, phone, email, active, created_by, tenant_id)
VALUES
    ('f1000000-0000-0000-0000-000000000001', '1234567',  'Carlos',   'Mamani Quispe',   'LC-001-2018', 'A-IV', '2027-06-30', '70012345', 'c.mamani@demo.local',   TRUE, 'seed', current_schema()),
    ('f1000000-0000-0000-0000-000000000002', '2345678',  'Pedro',    'Flores Condori',  'LC-002-2019', 'A-IV', '2026-12-31', '70023456', 'p.flores@demo.local',   TRUE, 'seed', current_schema()),
    ('f1000000-0000-0000-0000-000000000003', '3456789',  'Juan',     'Choque Apaza',    'LC-003-2017', 'A-III','2025-09-15', '70034567', 'j.choque@demo.local',   TRUE, 'seed', current_schema()),
    ('f1000000-0000-0000-0000-000000000004', '4567890',  'Marco',    'Quispe Laime',    'LC-004-2021', 'A-IV', '2028-03-20', '70045678', 'm.quispe@demo.local',   TRUE, 'seed', current_schema()),
    ('f1000000-0000-0000-0000-000000000005', '5678901',  'Roberto',  'Huanca Vargas',   'LC-005-2020', 'A-IV', '2027-11-10', '70056789', 'r.huanca@demo.local',   TRUE, 'seed', current_schema()),
    ('f1000000-0000-0000-0000-000000000006', '6789012',  'Luis',     'Torrez Gutierrez','LC-006-2022', 'A-IV', '2029-01-15', '70067890', 'l.torrez@demo.local',   TRUE, 'seed', current_schema()),
    ('f1000000-0000-0000-0000-000000000007', '7890123',  'Alvaro',   'Cusi Mendoza',    'LC-007-2016', 'A-III','2025-05-30', '70078901', 'a.cusi@demo.local',     FALSE,'seed', current_schema())  -- inactivo
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 7. RUTAS
--    (origin → destination, km, duración estimada, precio base en BOB)
-- -----------------------------------------------------------------------------
INSERT INTO routes (id, origin_location_id, destination_location_id,
                    distance_km, estimated_duration_minutes, base_price,
                    active, description, created_by, tenant_id)
VALUES
    -- La Paz ↔ Cochabamba  (~391 km, ~7 h)
    ('00700000-0000-0000-0000-000000000001',
     'd1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000002',
     391.00, 420, 55.00, TRUE, 'La Paz → Cochabamba (vía Caracollo)', 'seed', current_schema()),

    ('00700000-0000-0000-0000-000000000002',
     'd1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000001',
     391.00, 420, 55.00, TRUE, 'Cochabamba → La Paz (vía Caracollo)', 'seed', current_schema()),

    -- La Paz ↔ Oruro  (~230 km, ~4 h)
    ('00700000-0000-0000-0000-000000000003',
     'd1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000004',
     230.00, 240, 30.00, TRUE, 'La Paz → Oruro (carretera nueva)', 'seed', current_schema()),

    ('00700000-0000-0000-0000-000000000004',
     'd1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000001',
     230.00, 240, 30.00, TRUE, 'Oruro → La Paz (carretera nueva)', 'seed', current_schema()),

    -- Cochabamba ↔ Santa Cruz  (~499 km, ~9 h)
    ('00700000-0000-0000-0000-000000000005',
     'd1000000-0000-0000-0000-000000000002', 'd1000000-0000-0000-0000-000000000003',
     499.00, 540, 75.00, TRUE, 'Cochabamba → Santa Cruz (vía Bulo Bulo)', 'seed', current_schema()),

    ('00700000-0000-0000-0000-000000000006',
     'd1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000002',
     499.00, 540, 75.00, TRUE, 'Santa Cruz → Cochabamba (vía Bulo Bulo)', 'seed', current_schema()),

    -- Oruro ↔ Potosí  (~200 km, ~3.5 h)
    ('00700000-0000-0000-0000-000000000007',
     'd1000000-0000-0000-0000-000000000004', 'd1000000-0000-0000-0000-000000000005',
     200.00, 210, 35.00, TRUE, 'Oruro → Potosí', 'seed', current_schema()),

    ('00700000-0000-0000-0000-000000000008',
     'd1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000004',
     200.00, 210, 35.00, TRUE, 'Potosí → Oruro', 'seed', current_schema()),

    -- Sucre ↔ Potosí  (~160 km, ~3 h)
    ('00700000-0000-0000-0000-000000000009',
     'd1000000-0000-0000-0000-000000000006', 'd1000000-0000-0000-0000-000000000005',
     160.00, 180, 30.00, TRUE, 'Sucre → Potosí', 'seed', current_schema()),

    ('00700000-0000-0000-0000-000000000010',
     'd1000000-0000-0000-0000-000000000005', 'd1000000-0000-0000-0000-000000000006',
     160.00, 180, 30.00, TRUE, 'Potosí → Sucre', 'seed', current_schema()),

    -- La Paz → Santa Cruz (ruta larga, ~1100 km, ~18 h, nocturno cama)
    ('00700000-0000-0000-0000-000000000011',
     'd1000000-0000-0000-0000-000000000001', 'd1000000-0000-0000-0000-000000000003',
     1100.00, 1080, 130.00, TRUE, 'La Paz → Santa Cruz (vía Cochabamba, nocturno)', 'seed', current_schema()),

    ('00700000-0000-0000-0000-000000000012',
     'd1000000-0000-0000-0000-000000000003', 'd1000000-0000-0000-0000-000000000001',
     1100.00, 1080, 130.00, TRUE, 'Santa Cruz → La Paz (vía Cochabamba, nocturno)', 'seed', current_schema())
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 8. HORARIOS (schedules) + DÍAS DE OPERACIÓN (schedule_days)
--    Días: 1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes, 6=Sábado, 7=Domingo
-- -----------------------------------------------------------------------------

-- Limpiar días anteriores solo de los schedules de este seed (idempotencia)
DELETE FROM schedule_days
WHERE schedule_id IN (
    '00800000-0000-0000-0000-000000000001',
    '00800000-0000-0000-0000-000000000002',
    '00800000-0000-0000-0000-000000000003',
    '00800000-0000-0000-0000-000000000004',
    '00800000-0000-0000-0000-000000000005',
    '00800000-0000-0000-0000-000000000006',
    '00800000-0000-0000-0000-000000000007',
    '00800000-0000-0000-0000-000000000008',
    '00800000-0000-0000-0000-000000000009',
    '00800000-0000-0000-0000-000000000010',
    '00800000-0000-0000-0000-000000000011',
    '00800000-0000-0000-0000-000000000012'
);

INSERT INTO schedules (id, route_id, bus_id, driver_id,
                       departure_time, arrival_time,
                       active, notes, created_by, tenant_id)
VALUES
    -- La Paz → Cochabamba: mañana (08:00) y tarde (14:00), L-V-S
    ('00800000-0000-0000-0000-000000000001',
     '00700000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000001',
     '08:00', '15:00', TRUE, 'Salida matutina LP→CB', 'seed', current_schema()),

    ('00800000-0000-0000-0000-000000000002',
     '00700000-0000-0000-0000-000000000001', 'e1000000-0000-0000-0000-000000000006', 'f1000000-0000-0000-0000-000000000002',
     '14:00', '21:00', TRUE, 'Salida vespertina LP→CB', 'seed', current_schema()),

    -- Cochabamba → La Paz: mañana (07:30), todos los días
    ('00800000-0000-0000-0000-000000000003',
     '00700000-0000-0000-0000-000000000002', 'e1000000-0000-0000-0000-000000000002', 'f1000000-0000-0000-0000-000000000003',
     '07:30', '14:30', TRUE, 'Salida matutina CB→LP', 'seed', current_schema()),

    -- La Paz → Oruro: cada día (09:00 y 16:00)
    ('00800000-0000-0000-0000-000000000004',
     '00700000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000003', 'f1000000-0000-0000-0000-000000000004',
     '09:00', '13:00', TRUE, 'Salida matutina LP→OR', 'seed', current_schema()),

    ('00800000-0000-0000-0000-000000000005',
     '00700000-0000-0000-0000-000000000003', 'e1000000-0000-0000-0000-000000000007', 'f1000000-0000-0000-0000-000000000005',
     '16:00', '20:00', TRUE, 'Salida tarde LP→OR', 'seed', current_schema()),

    -- Oruro → La Paz: (10:00), L-Mi-V
    ('00800000-0000-0000-0000-000000000006',
     '00700000-0000-0000-0000-000000000004', 'e1000000-0000-0000-0000-000000000001', 'f1000000-0000-0000-0000-000000000006',
     '10:00', '14:00', TRUE, 'Oruro→La Paz, días alternos', 'seed', current_schema()),

    -- Cochabamba → Santa Cruz: nocturno (21:00), V-S-D
    ('00800000-0000-0000-0000-000000000007',
     '00700000-0000-0000-0000-000000000005', 'e1000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000001',
     '21:00', '06:00', TRUE, 'Nocturno cama CB→SCZ', 'seed', current_schema()),

    -- Santa Cruz → Cochabamba: nocturno (20:00), V-S-D
    ('00800000-0000-0000-0000-000000000008',
     '00700000-0000-0000-0000-000000000006', 'e1000000-0000-0000-0000-000000000005', 'f1000000-0000-0000-0000-000000000002',
     '20:00', '05:00', TRUE, 'Nocturno cama SCZ→CB', 'seed', current_schema()),

    -- Oruro → Potosí: (11:00), M-J-S
    ('00800000-0000-0000-0000-000000000009',
     '00700000-0000-0000-0000-000000000007', 'e1000000-0000-0000-0000-000000000006', 'f1000000-0000-0000-0000-000000000003',
     '11:00', '14:30', TRUE, 'OR→PT días alternos', 'seed', current_schema()),

    -- Potosí → Oruro: (12:00), M-J-S
    ('00800000-0000-0000-0000-000000000010',
     '00700000-0000-0000-0000-000000000008', 'e1000000-0000-0000-0000-000000000007', 'f1000000-0000-0000-0000-000000000004',
     '12:00', '15:30', TRUE, 'PT→OR días alternos', 'seed', current_schema()),

    -- La Paz → Santa Cruz: nocturno cama (19:00), D-Mi-V
    ('00800000-0000-0000-0000-000000000011',
     '00700000-0000-0000-0000-000000000011', 'e1000000-0000-0000-0000-000000000004', 'f1000000-0000-0000-0000-000000000005',
     '19:00', '13:00', TRUE, 'Nocturno cama LP→SCZ', 'seed', current_schema()),

    -- Santa Cruz → La Paz: nocturno cama (18:00), D-Mi-V
    ('00800000-0000-0000-0000-000000000012',
     '00700000-0000-0000-0000-000000000012', 'e1000000-0000-0000-0000-000000000005', 'f1000000-0000-0000-0000-000000000006',
     '18:00', '12:00', TRUE, 'Nocturno cama SCZ→LP', 'seed', current_schema())
ON CONFLICT (id) DO NOTHING;

-- Días de operación
INSERT INTO schedule_days (schedule_id, day_of_week) VALUES
    -- h1: LP→CB mañana — Lunes, Viernes, Sábado
    ('00800000-0000-0000-0000-000000000001', 1),
    ('00800000-0000-0000-0000-000000000001', 5),
    ('00800000-0000-0000-0000-000000000001', 6),
    -- h2: LP→CB tarde — Lunes a Sábado
    ('00800000-0000-0000-0000-000000000002', 1),
    ('00800000-0000-0000-0000-000000000002', 2),
    ('00800000-0000-0000-0000-000000000002', 3),
    ('00800000-0000-0000-0000-000000000002', 4),
    ('00800000-0000-0000-0000-000000000002', 5),
    ('00800000-0000-0000-0000-000000000002', 6),
    -- h3: CB→LP — todos los días
    ('00800000-0000-0000-0000-000000000003', 1),
    ('00800000-0000-0000-0000-000000000003', 2),
    ('00800000-0000-0000-0000-000000000003', 3),
    ('00800000-0000-0000-0000-000000000003', 4),
    ('00800000-0000-0000-0000-000000000003', 5),
    ('00800000-0000-0000-0000-000000000003', 6),
    ('00800000-0000-0000-0000-000000000003', 7),
    -- h4: LP→OR mañana — todos los días
    ('00800000-0000-0000-0000-000000000004', 1),
    ('00800000-0000-0000-0000-000000000004', 2),
    ('00800000-0000-0000-0000-000000000004', 3),
    ('00800000-0000-0000-0000-000000000004', 4),
    ('00800000-0000-0000-0000-000000000004', 5),
    ('00800000-0000-0000-0000-000000000004', 6),
    ('00800000-0000-0000-0000-000000000004', 7),
    -- h5: LP→OR tarde — todos los días
    ('00800000-0000-0000-0000-000000000005', 1),
    ('00800000-0000-0000-0000-000000000005', 2),
    ('00800000-0000-0000-0000-000000000005', 3),
    ('00800000-0000-0000-0000-000000000005', 4),
    ('00800000-0000-0000-0000-000000000005', 5),
    ('00800000-0000-0000-0000-000000000005', 6),
    ('00800000-0000-0000-0000-000000000005', 7),
    -- h6: OR→LP — Lunes, Miércoles, Viernes
    ('00800000-0000-0000-0000-000000000006', 1),
    ('00800000-0000-0000-0000-000000000006', 3),
    ('00800000-0000-0000-0000-000000000006', 5),
    -- h7: CB→SCZ nocturno — Viernes, Sábado, Domingo
    ('00800000-0000-0000-0000-000000000007', 5),
    ('00800000-0000-0000-0000-000000000007', 6),
    ('00800000-0000-0000-0000-000000000007', 7),
    -- h8: SCZ→CB nocturno — Viernes, Sábado, Domingo
    ('00800000-0000-0000-0000-000000000008', 5),
    ('00800000-0000-0000-0000-000000000008', 6),
    ('00800000-0000-0000-0000-000000000008', 7),
    -- h9: OR→PT — Martes, Jueves, Sábado
    ('00800000-0000-0000-0000-000000000009', 2),
    ('00800000-0000-0000-0000-000000000009', 4),
    ('00800000-0000-0000-0000-000000000009', 6),
    -- h10: PT→OR — Martes, Jueves, Sábado
    ('00800000-0000-0000-0000-000000000010', 2),
    ('00800000-0000-0000-0000-000000000010', 4),
    ('00800000-0000-0000-0000-000000000010', 6),
    -- h11: LP→SCZ nocturno — Miércoles, Viernes, Domingo
    ('00800000-0000-0000-0000-000000000011', 3),
    ('00800000-0000-0000-0000-000000000011', 5),
    ('00800000-0000-0000-0000-000000000011', 7),
    -- h12: SCZ→LP nocturno — Miércoles, Viernes, Domingo
    ('00800000-0000-0000-0000-000000000012', 3),
    ('00800000-0000-0000-0000-000000000012', 5),
    ('00800000-0000-0000-0000-000000000012', 7);

-- -----------------------------------------------------------------------------
-- Resumen
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    RAISE NOTICE '--- seed_test_operacion completado en esquema: % ---', current_schema();
    RAISE NOTICE 'Fleets   : 3';
    RAISE NOTICE 'Depart.  : 6  | Provincias: 6 | Ubicaciones: 6';
    RAISE NOTICE 'Buses    : 7  | Choferes  : 7 (1 inactivo)';
    RAISE NOTICE 'Rutas    : 12 (6 pares ida/vuelta)';
    RAISE NOTICE 'Horarios : 12 + días de operación';
END $$;
