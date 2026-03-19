-- =============================================================================
-- V7__seed_roles_resources.sql
-- Seed: recursos del sistema + 3 perfiles por defecto
--   ADMINISTRADOR  → acceso total
--   OPERADOR       → gestión operativa + supervisión
--   CAJERO         → venta de pasajes + caja + encomiendas
-- =============================================================================

DO $$
DECLARE
    -- Profiles
    p_admin   UUID;
    p_oper    UUID;
    p_cajero  UUID;

    -- ── USUARIOS ──────────────────────────────────────────────────────────────
    r_users_list        UUID;  r_users_get         UUID;
    r_users_create      UUID;  r_users_update      UUID;
    r_users_delete      UUID;
    r_profiles_list     UUID;  r_profiles_get      UUID;
    r_profiles_create   UUID;  r_profiles_update   UUID;
    r_profiles_res_add  UUID;  r_profiles_res_rem  UUID;
    r_profiles_delete   UUID;
    r_resources_list    UUID;  r_resources_get     UUID;
    r_resources_create  UUID;  r_resources_update  UUID;
    r_resources_delete  UUID;

    -- ── OPERACION ─────────────────────────────────────────────────────────────
    r_fleet_list        UUID;  r_fleet_get         UUID;
    r_fleet_create      UUID;  r_fleet_update      UUID;
    r_fleet_delete      UUID;
    r_buses_list        UUID;  r_buses_get         UUID;
    r_buses_create      UUID;  r_buses_update      UUID;
    r_buses_delete      UUID;
    r_drivers_list      UUID;  r_drivers_get       UUID;
    r_drivers_create    UUID;  r_drivers_update    UUID;
    r_drivers_delete    UUID;
    r_routes_list       UUID;  r_routes_get        UUID;
    r_routes_create     UUID;  r_routes_update     UUID;
    r_routes_delete     UUID;
    r_loc_list          UUID;  r_loc_get           UUID;
    r_loc_create        UUID;  r_loc_update        UUID;
    r_loc_delete        UUID;
    r_sched_list        UUID;  r_sched_get         UUID;
    r_sched_by_route    UUID;  r_sched_create      UUID;
    r_sched_update      UUID;  r_sched_delete      UUID;
    r_cust_list         UUID;  r_cust_get          UUID;
    r_cust_by_doc       UUID;  r_cust_create       UUID;
    r_cust_update       UUID;  r_cust_delete       UUID;

    -- ── PASAJES ───────────────────────────────────────────────────────────────
    r_tick_list         UUID;  r_tick_get          UUID;
    r_tick_by_code      UUID;  r_tick_create       UUID;
    r_tick_bulk         UUID;
    r_tick_upd_cust     UUID;  r_tick_upd_seat     UUID;
    r_tick_cancel       UUID;
    r_seats_map         UUID;  r_seats_count       UUID;
    r_res_create        UUID;  r_res_confirm       UUID;
    r_res_cancel        UUID;
    r_resch_list        UUID;  r_resch_create      UUID;
    r_refund_list       UUID;  r_refund_create     UUID;
    r_refund_approve    UUID;  r_refund_reject     UUID;

    -- ── ENCOMIENDAS ───────────────────────────────────────────────────────────
    r_parc_list         UUID;  r_parc_by_status    UUID;
    r_parc_get          UUID;  r_parc_by_track     UUID;
    r_parc_track_id     UUID;  r_parc_track_hist   UUID;
    r_parc_create       UUID;  r_parc_upd_status   UUID;

    -- ── FINANZAS ──────────────────────────────────────────────────────────────
    r_cash_list         UUID;  r_cash_get          UUID;
    r_cash_summary      UUID;  r_cash_open         UUID;
    r_cash_close        UUID;
    r_txn_by_reg        UUID;  r_txn_create        UUID;
    r_inv_list          UUID;  r_inv_get           UUID;
    r_inv_create        UUID;  r_inv_cancel        UUID;
    r_param_list        UUID;  r_param_get         UUID;
    r_param_create      UUID;  r_param_update      UUID;

    -- ── TENANTS ───────────────────────────────────────────────────────────────
    r_ten_list          UUID;  r_ten_get           UUID;
    r_ten_by_schema     UUID;  r_ten_create        UUID;
    r_ten_update        UUID;  r_ten_status        UUID;

    -- ── AUDITORIA ─────────────────────────────────────────────────────────────
    r_audit_list        UUID;

    -- ── SIAT ──────────────────────────────────────────────────────────────────
    r_siat_cfg_list     UUID;  r_siat_cfg_get      UUID;
    r_siat_cfg_create   UUID;  r_siat_cfg_update   UUID;
    r_siat_cfg_toggle   UUID;
    r_siat_cuis_obtain  UUID;  r_siat_cuis_vigente UUID;
    r_siat_cufd_obtain  UUID;  r_siat_cufd_vigente UUID;
    r_siat_cat_list     UUID;  r_siat_cat_sync     UUID;
    r_siat_emit_get     UUID;  r_siat_emit_by_inv  UUID;
    r_siat_emit_by_cuf  UUID;  r_siat_emit_estado  UUID;
    r_siat_emit_indiv   UUID;
    r_siat_emit_paquete UUID;  r_siat_emit_paq_val UUID;
    r_siat_emit_masiva  UUID;  r_siat_emit_mas_val UUID;
    r_siat_anular       UUID;  r_siat_revertir     UUID;
    r_siat_evt_list     UUID;  r_siat_evt_create   UUID;

BEGIN

-- =============================================================================
-- 1. RECURSOS
-- =============================================================================

    -- ── USUARIOS ──────────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar usuarios',            'GET',    '/api/v1/users',                      'USUARIOS',    'Lista paginada de usuarios activos')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_users_list;
    IF r_users_list IS NULL THEN SELECT id INTO r_users_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/users'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver usuario',                'GET',    '/api/v1/users/{id}',                 'USUARIOS',    'Obtiene un usuario por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_users_get;
    IF r_users_get IS NULL THEN SELECT id INTO r_users_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/users/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear usuario',              'POST',   '/api/v1/users',                      'USUARIOS',    'Crea un nuevo usuario')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_users_create;
    IF r_users_create IS NULL THEN SELECT id INTO r_users_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/users'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar usuario',         'PUT',    '/api/v1/users/{id}',                 'USUARIOS',    'Actualiza datos de un usuario')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_users_update;
    IF r_users_update IS NULL THEN SELECT id INTO r_users_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/users/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar usuario',         'DELETE', '/api/v1/users/{id}',                 'USUARIOS',    'Desactiva un usuario')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_users_delete;
    IF r_users_delete IS NULL THEN SELECT id INTO r_users_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/users/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar perfiles',            'GET',    '/api/v1/profiles',                   'USUARIOS',    'Lista paginada de perfiles')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_profiles_list;
    IF r_profiles_list IS NULL THEN SELECT id INTO r_profiles_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/profiles'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver perfil',                 'GET',    '/api/v1/profiles/{id}',              'USUARIOS',    'Obtiene un perfil con sus recursos')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_profiles_get;
    IF r_profiles_get IS NULL THEN SELECT id INTO r_profiles_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/profiles/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear perfil',               'POST',   '/api/v1/profiles',                   'USUARIOS',    'Crea un nuevo perfil')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_profiles_create;
    IF r_profiles_create IS NULL THEN SELECT id INTO r_profiles_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/profiles'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar perfil',          'PUT',    '/api/v1/profiles/{id}',              'USUARIOS',    'Actualiza datos de un perfil')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_profiles_update;
    IF r_profiles_update IS NULL THEN SELECT id INTO r_profiles_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/profiles/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Asignar recursos a perfil',  'POST',   '/api/v1/profiles/{id}/resources',    'USUARIOS',    'Asigna recursos a un perfil')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_profiles_res_add;
    IF r_profiles_res_add IS NULL THEN SELECT id INTO r_profiles_res_add FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/profiles/{id}/resources'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Quitar recursos de perfil',  'DELETE', '/api/v1/profiles/{id}/resources',    'USUARIOS',    'Quita recursos de un perfil')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_profiles_res_rem;
    IF r_profiles_res_rem IS NULL THEN SELECT id INTO r_profiles_res_rem FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/profiles/{id}/resources'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar perfil',          'DELETE', '/api/v1/profiles/{id}',              'USUARIOS',    'Desactiva un perfil')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_profiles_delete;
    IF r_profiles_delete IS NULL THEN SELECT id INTO r_profiles_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/profiles/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar recursos',            'GET',    '/api/v1/resources',                  'USUARIOS',    'Lista paginada de recursos del sistema')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_resources_list;
    IF r_resources_list IS NULL THEN SELECT id INTO r_resources_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/resources'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver recurso',                'GET',    '/api/v1/resources/{id}',             'USUARIOS',    'Obtiene un recurso por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_resources_get;
    IF r_resources_get IS NULL THEN SELECT id INTO r_resources_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/resources/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear recurso',              'POST',   '/api/v1/resources',                  'USUARIOS',    'Registra un nuevo recurso')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_resources_create;
    IF r_resources_create IS NULL THEN SELECT id INTO r_resources_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/resources'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar recurso',         'PUT',    '/api/v1/resources/{id}',             'USUARIOS',    'Actualiza un recurso')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_resources_update;
    IF r_resources_update IS NULL THEN SELECT id INTO r_resources_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/resources/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar recurso',         'DELETE', '/api/v1/resources/{id}',             'USUARIOS',    'Desactiva un recurso')
        ON CONFLICT (http_method, endpoint) DO NOTHING
        RETURNING id INTO r_resources_delete;
    IF r_resources_delete IS NULL THEN SELECT id INTO r_resources_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/resources/{id}'; END IF;

    -- ── OPERACION ─────────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar flotas',              'GET',    '/api/v1/fleets',                     'OPERACION',   'Lista paginada de flotas')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_fleet_list;
    IF r_fleet_list IS NULL THEN SELECT id INTO r_fleet_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/fleets'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver flota',                  'GET',    '/api/v1/fleets/{id}',                'OPERACION',   'Obtiene una flota por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_fleet_get;
    IF r_fleet_get IS NULL THEN SELECT id INTO r_fleet_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/fleets/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear flota',                'POST',   '/api/v1/fleets',                     'OPERACION',   'Registra una nueva flota')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_fleet_create;
    IF r_fleet_create IS NULL THEN SELECT id INTO r_fleet_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/fleets'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar flota',           'PUT',    '/api/v1/fleets/{id}',                'OPERACION',   'Actualiza datos de una flota')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_fleet_update;
    IF r_fleet_update IS NULL THEN SELECT id INTO r_fleet_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/fleets/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar flota',           'DELETE', '/api/v1/fleets/{id}',                'OPERACION',   'Desactiva una flota')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_fleet_delete;
    IF r_fleet_delete IS NULL THEN SELECT id INTO r_fleet_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/fleets/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar buses',               'GET',    '/api/v1/buses',                      'OPERACION',   'Lista paginada de buses activos')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_buses_list;
    IF r_buses_list IS NULL THEN SELECT id INTO r_buses_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/buses'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver bus',                    'GET',    '/api/v1/buses/{id}',                 'OPERACION',   'Obtiene un bus por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_buses_get;
    IF r_buses_get IS NULL THEN SELECT id INTO r_buses_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/buses/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear bus',                  'POST',   '/api/v1/buses',                      'OPERACION',   'Registra un nuevo bus')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_buses_create;
    IF r_buses_create IS NULL THEN SELECT id INTO r_buses_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/buses'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar bus',             'PUT',    '/api/v1/buses/{id}',                 'OPERACION',   'Actualiza datos de un bus')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_buses_update;
    IF r_buses_update IS NULL THEN SELECT id INTO r_buses_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/buses/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar bus',             'DELETE', '/api/v1/buses/{id}',                 'OPERACION',   'Desactiva un bus')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_buses_delete;
    IF r_buses_delete IS NULL THEN SELECT id INTO r_buses_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/buses/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar conductores',         'GET',    '/api/v1/drivers',                    'OPERACION',   'Lista paginada de conductores activos')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_drivers_list;
    IF r_drivers_list IS NULL THEN SELECT id INTO r_drivers_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/drivers'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver conductor',              'GET',    '/api/v1/drivers/{id}',               'OPERACION',   'Obtiene un conductor por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_drivers_get;
    IF r_drivers_get IS NULL THEN SELECT id INTO r_drivers_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/drivers/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear conductor',            'POST',   '/api/v1/drivers',                    'OPERACION',   'Registra un nuevo conductor')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_drivers_create;
    IF r_drivers_create IS NULL THEN SELECT id INTO r_drivers_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/drivers'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar conductor',       'PUT',    '/api/v1/drivers/{id}',               'OPERACION',   'Actualiza datos de un conductor')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_drivers_update;
    IF r_drivers_update IS NULL THEN SELECT id INTO r_drivers_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/drivers/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar conductor',       'DELETE', '/api/v1/drivers/{id}',               'OPERACION',   'Desactiva un conductor')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_drivers_delete;
    IF r_drivers_delete IS NULL THEN SELECT id INTO r_drivers_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/drivers/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar rutas',               'GET',    '/api/v1/routes',                     'OPERACION',   'Lista paginada de rutas activas')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_routes_list;
    IF r_routes_list IS NULL THEN SELECT id INTO r_routes_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/routes'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver ruta',                   'GET',    '/api/v1/routes/{id}',                'OPERACION',   'Obtiene una ruta por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_routes_get;
    IF r_routes_get IS NULL THEN SELECT id INTO r_routes_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/routes/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear ruta',                 'POST',   '/api/v1/routes',                     'OPERACION',   'Registra una nueva ruta')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_routes_create;
    IF r_routes_create IS NULL THEN SELECT id INTO r_routes_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/routes'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar ruta',            'PUT',    '/api/v1/routes/{id}',                'OPERACION',   'Actualiza datos de una ruta')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_routes_update;
    IF r_routes_update IS NULL THEN SELECT id INTO r_routes_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/routes/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar ruta',            'DELETE', '/api/v1/routes/{id}',                'OPERACION',   'Desactiva una ruta')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_routes_delete;
    IF r_routes_delete IS NULL THEN SELECT id INTO r_routes_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/routes/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar ubicaciones',         'GET',    '/api/v1/locations',                  'OPERACION',   'Lista paginada de ubicaciones activas')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_loc_list;
    IF r_loc_list IS NULL THEN SELECT id INTO r_loc_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/locations'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver ubicación',              'GET',    '/api/v1/locations/{id}',              'OPERACION',   'Obtiene una ubicación por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_loc_get;
    IF r_loc_get IS NULL THEN SELECT id INTO r_loc_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/locations/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear ubicación',            'POST',   '/api/v1/locations',                  'OPERACION',   'Registra una nueva ubicación')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_loc_create;
    IF r_loc_create IS NULL THEN SELECT id INTO r_loc_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/locations'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar ubicación',       'PUT',    '/api/v1/locations/{id}',              'OPERACION',   'Actualiza datos de una ubicación')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_loc_update;
    IF r_loc_update IS NULL THEN SELECT id INTO r_loc_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/locations/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar ubicación',       'DELETE', '/api/v1/locations/{id}',              'OPERACION',   'Desactiva una ubicación')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_loc_delete;
    IF r_loc_delete IS NULL THEN SELECT id INTO r_loc_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/locations/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar horarios',            'GET',    '/api/v1/schedules',                  'OPERACION',   'Lista paginada de horarios activos')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_sched_list;
    IF r_sched_list IS NULL THEN SELECT id INTO r_sched_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/schedules'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver horario',                'GET',    '/api/v1/schedules/{id}',             'OPERACION',   'Obtiene un horario por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_sched_get;
    IF r_sched_get IS NULL THEN SELECT id INTO r_sched_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/schedules/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Horarios por ruta',          'GET',    '/api/v1/schedules/route/{routeId}',  'OPERACION',   'Lista horarios de una ruta')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_sched_by_route;
    IF r_sched_by_route IS NULL THEN SELECT id INTO r_sched_by_route FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/schedules/route/{routeId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear horario',              'POST',   '/api/v1/schedules',                  'OPERACION',   'Registra un nuevo horario')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_sched_create;
    IF r_sched_create IS NULL THEN SELECT id INTO r_sched_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/schedules'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar horario',         'PUT',    '/api/v1/schedules/{id}',             'OPERACION',   'Actualiza un horario')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_sched_update;
    IF r_sched_update IS NULL THEN SELECT id INTO r_sched_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/schedules/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar horario',         'DELETE', '/api/v1/schedules/{id}',             'OPERACION',   'Desactiva un horario')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_sched_delete;
    IF r_sched_delete IS NULL THEN SELECT id INTO r_sched_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/schedules/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar clientes',            'GET',    '/api/v1/customers',                  'OPERACION',   'Lista paginada de clientes activos')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cust_list;
    IF r_cust_list IS NULL THEN SELECT id INTO r_cust_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/customers'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver cliente',                'GET',    '/api/v1/customers/{id}',             'OPERACION',   'Obtiene un cliente por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cust_get;
    IF r_cust_get IS NULL THEN SELECT id INTO r_cust_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/customers/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Buscar cliente por documento','GET',   '/api/v1/customers/document/{documentNumber}','OPERACION','Busca un cliente por nro de documento')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cust_by_doc;
    IF r_cust_by_doc IS NULL THEN SELECT id INTO r_cust_by_doc FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/customers/document/{documentNumber}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear cliente',              'POST',   '/api/v1/customers',                  'OPERACION',   'Registra un nuevo cliente')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cust_create;
    IF r_cust_create IS NULL THEN SELECT id INTO r_cust_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/customers'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar cliente',         'PUT',    '/api/v1/customers/{id}',             'OPERACION',   'Actualiza datos de un cliente')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cust_update;
    IF r_cust_update IS NULL THEN SELECT id INTO r_cust_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/customers/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Desactivar cliente',         'DELETE', '/api/v1/customers/{id}',             'OPERACION',   'Desactiva un cliente')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cust_delete;
    IF r_cust_delete IS NULL THEN SELECT id INTO r_cust_delete FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/customers/{id}'; END IF;

    -- ── PASAJES ───────────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar pasajes',             'GET',    '/api/v1/tickets',                    'PASAJES',     'Lista paginada de pasajes')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_list;
    IF r_tick_list IS NULL THEN SELECT id INTO r_tick_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/tickets'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver pasaje',                 'GET',    '/api/v1/tickets/{id}',               'PASAJES',     'Obtiene un pasaje por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_get;
    IF r_tick_get IS NULL THEN SELECT id INTO r_tick_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/tickets/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Buscar pasaje por código',   'GET',    '/api/v1/tickets/code/{code}',        'PASAJES',     'Obtiene un pasaje por código')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_by_code;
    IF r_tick_by_code IS NULL THEN SELECT id INTO r_tick_by_code FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/tickets/code/{code}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Vender pasaje',              'POST',   '/api/v1/tickets',                    'PASAJES',     'Emite un nuevo pasaje')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_create;
    IF r_tick_create IS NULL THEN SELECT id INTO r_tick_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/tickets'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Venta múltiple de pasajes',  'POST',   '/api/v1/tickets/bulk',               'PASAJES',     'Emite múltiples pasajes en una sola transacción atómica')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_bulk;
    IF r_tick_bulk IS NULL THEN SELECT id INTO r_tick_bulk FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/tickets/bulk'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar pasajero',        'PATCH',  '/api/v1/tickets/{id}/customer',      'PASAJES',     'Modifica datos del pasajero en un pasaje')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_upd_cust;
    IF r_tick_upd_cust IS NULL THEN SELECT id INTO r_tick_upd_cust FROM resources WHERE http_method='PATCH'  AND endpoint='/api/v1/tickets/{id}/customer'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Cambiar asiento',            'PATCH',  '/api/v1/tickets/{id}/seat',          'PASAJES',     'Cambia el asiento de un pasaje')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_upd_seat;
    IF r_tick_upd_seat IS NULL THEN SELECT id INTO r_tick_upd_seat FROM resources WHERE http_method='PATCH'  AND endpoint='/api/v1/tickets/{id}/seat'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Cancelar pasaje',            'DELETE', '/api/v1/tickets/{id}',               'PASAJES',     'Cancela un pasaje')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_tick_cancel;
    IF r_tick_cancel IS NULL THEN SELECT id INTO r_tick_cancel FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/tickets/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver mapa de asientos',       'GET',    '/api/v1/seat-maps/{scheduleId}',     'PASAJES',     'Mapa de asientos para un horario y fecha')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_seats_map;
    IF r_seats_map IS NULL THEN SELECT id INTO r_seats_map FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/seat-maps/{scheduleId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Contar asientos disponibles','GET',    '/api/v1/seat-maps/{scheduleId}/available-count','PASAJES','Cuenta asientos disponibles')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_seats_count;
    IF r_seats_count IS NULL THEN SELECT id INTO r_seats_count FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/seat-maps/{scheduleId}/available-count'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear reserva',              'POST',   '/api/v1/reservations',               'PASAJES',     'Reserva un asiento temporalmente')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_res_create;
    IF r_res_create IS NULL THEN SELECT id INTO r_res_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/reservations'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Confirmar reserva',          'POST',   '/api/v1/reservations/{id}/confirm',  'PASAJES',     'Confirma una reserva activa')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_res_confirm;
    IF r_res_confirm IS NULL THEN SELECT id INTO r_res_confirm FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/reservations/{id}/confirm'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Cancelar reserva',           'DELETE', '/api/v1/reservations/{id}',          'PASAJES',     'Cancela una reserva')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_res_cancel;
    IF r_res_cancel IS NULL THEN SELECT id INTO r_res_cancel FROM resources WHERE http_method='DELETE' AND endpoint='/api/v1/reservations/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar recambios',           'GET',    '/api/v1/reschedules',                'PASAJES',     'Lista paginada de recambios')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_resch_list;
    IF r_resch_list IS NULL THEN SELECT id INTO r_resch_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/reschedules'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Realizar recambio',          'POST',   '/api/v1/reschedules',                'PASAJES',     'Realiza el recambio de un pasaje')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_resch_create;
    IF r_resch_create IS NULL THEN SELECT id INTO r_resch_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/reschedules'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar reembolsos',          'GET',    '/api/v1/refunds',                    'PASAJES',     'Lista paginada de solicitudes de reembolso')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_refund_list;
    IF r_refund_list IS NULL THEN SELECT id INTO r_refund_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/refunds'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Solicitar reembolso',        'POST',   '/api/v1/refunds',                    'PASAJES',     'Solicita el reembolso de un pasaje')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_refund_create;
    IF r_refund_create IS NULL THEN SELECT id INTO r_refund_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/refunds'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Aprobar reembolso',          'POST',   '/api/v1/refunds/{id}/approve',       'PASAJES',     'Aprueba una solicitud de reembolso')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_refund_approve;
    IF r_refund_approve IS NULL THEN SELECT id INTO r_refund_approve FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/refunds/{id}/approve'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Rechazar reembolso',         'POST',   '/api/v1/refunds/{id}/reject',        'PASAJES',     'Rechaza una solicitud de reembolso')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_refund_reject;
    IF r_refund_reject IS NULL THEN SELECT id INTO r_refund_reject FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/refunds/{id}/reject'; END IF;

    -- ── ENCOMIENDAS ───────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar encomiendas',         'GET',    '/api/v1/parcels',                    'ENCOMIENDAS', 'Lista paginada de encomiendas')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_list;
    IF r_parc_list IS NULL THEN SELECT id INTO r_parc_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parcels'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Encomiendas por estado',     'GET',    '/api/v1/parcels/status/{status}',    'ENCOMIENDAS', 'Lista encomiendas filtradas por estado')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_by_status;
    IF r_parc_by_status IS NULL THEN SELECT id INTO r_parc_by_status FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parcels/status/{status}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver encomienda',             'GET',    '/api/v1/parcels/{id}',               'ENCOMIENDAS', 'Obtiene una encomienda por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_get;
    IF r_parc_get IS NULL THEN SELECT id INTO r_parc_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parcels/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Buscar por código de rastreo','GET',   '/api/v1/parcels/tracking/{trackingCode}','ENCOMIENDAS','Busca encomienda por código de rastreo')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_by_track;
    IF r_parc_by_track IS NULL THEN SELECT id INTO r_parc_by_track FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parcels/tracking/{trackingCode}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Historial de rastreo por ID','GET',    '/api/v1/parcels/{id}/tracking',      'ENCOMIENDAS', 'Historial de rastreo de una encomienda')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_track_id;
    IF r_parc_track_id IS NULL THEN SELECT id INTO r_parc_track_id FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parcels/{id}/tracking'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Historial público de rastreo','GET',   '/api/v1/parcels/tracking/{trackingCode}/history','ENCOMIENDAS','Historial público por código de rastreo')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_track_hist;
    IF r_parc_track_hist IS NULL THEN SELECT id INTO r_parc_track_hist FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parcels/tracking/{trackingCode}/history'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Registrar encomienda',       'POST',   '/api/v1/parcels',                    'ENCOMIENDAS', 'Registra una nueva encomienda')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_create;
    IF r_parc_create IS NULL THEN SELECT id INTO r_parc_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/parcels'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar estado encomienda','PATCH',  '/api/v1/parcels/{id}/status',       'ENCOMIENDAS', 'Cambia el estado de una encomienda')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_parc_upd_status;
    IF r_parc_upd_status IS NULL THEN SELECT id INTO r_parc_upd_status FROM resources WHERE http_method='PATCH'  AND endpoint='/api/v1/parcels/{id}/status'; END IF;

    -- ── FINANZAS ──────────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar cajas',               'GET',    '/api/v1/cash-registers',             'FINANZAS',    'Lista paginada de cajas registradoras')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cash_list;
    IF r_cash_list IS NULL THEN SELECT id INTO r_cash_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/cash-registers'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver caja',                   'GET',    '/api/v1/cash-registers/{id}',        'FINANZAS',    'Obtiene una caja registradora por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cash_get;
    IF r_cash_get IS NULL THEN SELECT id INTO r_cash_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/cash-registers/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Resumen de caja',            'GET',    '/api/v1/cash-registers/{id}/summary','FINANZAS',    'Obtiene el resumen/balance de una caja')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cash_summary;
    IF r_cash_summary IS NULL THEN SELECT id INTO r_cash_summary FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/cash-registers/{id}/summary'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Abrir caja',                 'POST',   '/api/v1/cash-registers/open',        'FINANZAS',    'Abre una nueva caja registradora')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cash_open;
    IF r_cash_open IS NULL THEN SELECT id INTO r_cash_open FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/cash-registers/open'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Cerrar caja',                'POST',   '/api/v1/cash-registers/{id}/close',  'FINANZAS',    'Cierra una caja registradora')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_cash_close;
    IF r_cash_close IS NULL THEN SELECT id INTO r_cash_close FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/cash-registers/{id}/close'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver transacciones de caja',  'GET',    '/api/v1/cash-transactions/register/{cashRegisterId}','FINANZAS','Lista transacciones de una caja')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_txn_by_reg;
    IF r_txn_by_reg IS NULL THEN SELECT id INTO r_txn_by_reg FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/cash-transactions/register/{cashRegisterId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Registrar transacción',      'POST',   '/api/v1/cash-transactions',          'FINANZAS',    'Registra una transacción en caja')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_txn_create;
    IF r_txn_create IS NULL THEN SELECT id INTO r_txn_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/cash-transactions'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar facturas',            'GET',    '/api/v1/invoices',                   'FINANZAS',    'Lista paginada de facturas')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_inv_list;
    IF r_inv_list IS NULL THEN SELECT id INTO r_inv_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/invoices'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver factura',                'GET',    '/api/v1/invoices/{id}',              'FINANZAS',    'Obtiene una factura por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_inv_get;
    IF r_inv_get IS NULL THEN SELECT id INTO r_inv_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/invoices/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear factura',              'POST',   '/api/v1/invoices',                   'FINANZAS',    'Emite una nueva factura')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_inv_create;
    IF r_inv_create IS NULL THEN SELECT id INTO r_inv_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/invoices'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Anular factura',             'POST',   '/api/v1/invoices/{id}/cancel',       'FINANZAS',    'Anula una factura emitida')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_inv_cancel;
    IF r_inv_cancel IS NULL THEN SELECT id INTO r_inv_cancel FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/invoices/{id}/cancel'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar parámetros',          'GET',    '/api/v1/parameters',                 'FINANZAS',    'Lista todos los parámetros del sistema')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_param_list;
    IF r_param_list IS NULL THEN SELECT id INTO r_param_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parameters'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver parámetro',              'GET',    '/api/v1/parameters/{id}',            'FINANZAS',    'Obtiene un parámetro por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_param_get;
    IF r_param_get IS NULL THEN SELECT id INTO r_param_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/parameters/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear parámetro',            'POST',   '/api/v1/parameters',                 'FINANZAS',    'Crea un parámetro del sistema')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_param_create;
    IF r_param_create IS NULL THEN SELECT id INTO r_param_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/parameters'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar parámetro',       'PUT',    '/api/v1/parameters/{id}',            'FINANZAS',    'Actualiza un parámetro del sistema')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_param_update;
    IF r_param_update IS NULL THEN SELECT id INTO r_param_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/parameters/{id}'; END IF;

    -- ── TENANTS ───────────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar tenants',             'GET',    '/api/v1/tenants',                    'TENANTS',     'Lista paginada de tenants')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_ten_list;
    IF r_ten_list IS NULL THEN SELECT id INTO r_ten_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/tenants'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver tenant',                 'GET',    '/api/v1/tenants/{id}',               'TENANTS',     'Obtiene un tenant por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_ten_get;
    IF r_ten_get IS NULL THEN SELECT id INTO r_ten_get FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/tenants/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Buscar tenant por esquema',  'GET',    '/api/v1/tenants/schema/{schemaName}','TENANTS',     'Busca un tenant por nombre de esquema')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_ten_by_schema;
    IF r_ten_by_schema IS NULL THEN SELECT id INTO r_ten_by_schema FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/tenants/schema/{schemaName}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear tenant',               'POST',   '/api/v1/tenants',                    'TENANTS',     'Crea un nuevo tenant y provisiona su esquema')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_ten_create;
    IF r_ten_create IS NULL THEN SELECT id INTO r_ten_create FROM resources WHERE http_method='POST'   AND endpoint='/api/v1/tenants'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar tenant',          'PUT',    '/api/v1/tenants/{id}',               'TENANTS',     'Actualiza datos de un tenant')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_ten_update;
    IF r_ten_update IS NULL THEN SELECT id INTO r_ten_update FROM resources WHERE http_method='PUT'    AND endpoint='/api/v1/tenants/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Cambiar estado tenant',      'PATCH',  '/api/v1/tenants/{id}/status',        'TENANTS',     'Activa o suspende un tenant')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_ten_status;
    IF r_ten_status IS NULL THEN SELECT id INTO r_ten_status FROM resources WHERE http_method='PATCH'  AND endpoint='/api/v1/tenants/{id}/status'; END IF;

    -- ── AUDITORIA ─────────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver logs de auditoría',      'GET',    '/api/v1/audit',                      'AUDITORIA',   'Consulta el log de auditoría con filtros')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_audit_list;
    IF r_audit_list IS NULL THEN SELECT id INTO r_audit_list FROM resources WHERE http_method='GET'    AND endpoint='/api/v1/audit'; END IF;

    -- ── SIAT ──────────────────────────────────────────────────────────────────
    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar configuraciones SIAT',  'GET',   '/api/v1/siat/config',                           'SIAT', 'Lista configuraciones SIAT por tenant')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cfg_list;
    IF r_siat_cfg_list IS NULL THEN SELECT id INTO r_siat_cfg_list FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/config'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver configuración SIAT',       'GET',   '/api/v1/siat/config/{id}',                      'SIAT', 'Obtiene una configuración SIAT por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cfg_get;
    IF r_siat_cfg_get IS NULL THEN SELECT id INTO r_siat_cfg_get FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/config/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Crear configuración SIAT',     'POST',  '/api/v1/siat/config',                           'SIAT', 'Registra una nueva configuración SIAT')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cfg_create;
    IF r_siat_cfg_create IS NULL THEN SELECT id INTO r_siat_cfg_create FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/config'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Actualizar configuración SIAT','PUT',   '/api/v1/siat/config/{id}',                      'SIAT', 'Actualiza una configuración SIAT')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cfg_update;
    IF r_siat_cfg_update IS NULL THEN SELECT id INTO r_siat_cfg_update FROM resources WHERE http_method='PUT'   AND endpoint='/api/v1/siat/config/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Activar/desactivar config SIAT','PATCH','/api/v1/siat/config/{id}/toggle',               'SIAT', 'Activa o desactiva una configuración SIAT')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cfg_toggle;
    IF r_siat_cfg_toggle IS NULL THEN SELECT id INTO r_siat_cfg_toggle FROM resources WHERE http_method='PATCH'  AND endpoint='/api/v1/siat/config/{id}/toggle'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Obtener CUIS del SIN',         'POST',  '/api/v1/siat/codigos/cuis/{configId}',          'SIAT', 'Solicita un nuevo CUIS al SIN Bolivia')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cuis_obtain;
    IF r_siat_cuis_obtain IS NULL THEN SELECT id INTO r_siat_cuis_obtain FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/codigos/cuis/{configId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Consultar CUIS vigente',       'GET',   '/api/v1/siat/codigos/cuis/{configId}/vigente',  'SIAT', 'Obtiene el CUIS vigente almacenado')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cuis_vigente;
    IF r_siat_cuis_vigente IS NULL THEN SELECT id INTO r_siat_cuis_vigente FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/codigos/cuis/{configId}/vigente'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Obtener CUFD del SIN',         'POST',  '/api/v1/siat/codigos/cufd/{configId}',          'SIAT', 'Solicita un nuevo CUFD al SIN Bolivia')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cufd_obtain;
    IF r_siat_cufd_obtain IS NULL THEN SELECT id INTO r_siat_cufd_obtain FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/codigos/cufd/{configId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Consultar CUFD vigente',       'GET',   '/api/v1/siat/codigos/cufd/{configId}/vigente',  'SIAT', 'Obtiene el CUFD vigente almacenado')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cufd_vigente;
    IF r_siat_cufd_vigente IS NULL THEN SELECT id INTO r_siat_cufd_vigente FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/codigos/cufd/{configId}/vigente'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar catálogos SIAT',        'GET',   '/api/v1/siat/catalogos',                        'SIAT', 'Lista los catálogos sincronizados del SIN')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cat_list;
    IF r_siat_cat_list IS NULL THEN SELECT id INTO r_siat_cat_list FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/catalogos'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Sincronizar catálogos SIAT',   'POST',  '/api/v1/siat/catalogos/sincronizar/{configId}', 'SIAT', 'Sincroniza catálogos desde el SIN Bolivia')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_cat_sync;
    IF r_siat_cat_sync IS NULL THEN SELECT id INTO r_siat_cat_sync FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/catalogos/sincronizar/{configId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver factura SIAT por ID',      'GET',   '/api/v1/siat/emision/{id}',                     'SIAT', 'Obtiene una factura SIAT por ID')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_get;
    IF r_siat_emit_get IS NULL THEN SELECT id INTO r_siat_emit_get FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/emision/{id}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver factura SIAT por invoice', 'GET',   '/api/v1/siat/emision/invoice/{invoiceId}',      'SIAT', 'Obtiene una factura SIAT por ID de factura interna')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_by_inv;
    IF r_siat_emit_by_inv IS NULL THEN SELECT id INTO r_siat_emit_by_inv FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/emision/invoice/{invoiceId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Ver factura SIAT por CUF',     'GET',   '/api/v1/siat/emision/cuf/{cuf}',               'SIAT', 'Obtiene una factura SIAT por CUF')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_by_cuf;
    IF r_siat_emit_by_cuf IS NULL THEN SELECT id INTO r_siat_emit_by_cuf FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/emision/cuf/{cuf}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Consultar estado factura SIAT','GET',   '/api/v1/siat/emision/{id}/estado',              'SIAT', 'Consulta el estado de una factura en el SIN')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_estado;
    IF r_siat_emit_estado IS NULL THEN SELECT id INTO r_siat_emit_estado FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/emision/{id}/estado'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Emisión individual SIAT',      'POST',  '/api/v1/siat/emision/individual',               'SIAT', 'Emite una factura individual en línea al SIN')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_indiv;
    IF r_siat_emit_indiv IS NULL THEN SELECT id INTO r_siat_emit_indiv FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/emision/individual'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Emisión offline en paquete',   'POST',  '/api/v1/siat/emision/paquete/{configId}',       'SIAT', 'Emite facturas en paquete comprimido (offline)')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_paquete;
    IF r_siat_emit_paquete IS NULL THEN SELECT id INTO r_siat_emit_paquete FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/emision/paquete/{configId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Validar paquete SIAT',         'POST',  '/api/v1/siat/emision/paquete/{paqueteId}/validar','SIAT','Valida un paquete enviado al SIN')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_paq_val;
    IF r_siat_emit_paq_val IS NULL THEN SELECT id INTO r_siat_emit_paq_val FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/emision/paquete/{paqueteId}/validar'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Emisión masiva SIAT',          'POST',  '/api/v1/siat/emision/masiva/{configId}',        'SIAT', 'Procesa todas las facturas pendientes de un paquete')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_masiva;
    IF r_siat_emit_masiva IS NULL THEN SELECT id INTO r_siat_emit_masiva FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/emision/masiva/{configId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Validar emisión masiva SIAT',  'POST',  '/api/v1/siat/emision/masiva/{paqueteId}/validar','SIAT','Valida el resultado de una emisión masiva')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_emit_mas_val;
    IF r_siat_emit_mas_val IS NULL THEN SELECT id INTO r_siat_emit_mas_val FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/emision/masiva/{paqueteId}/validar'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Anular factura SIAT',          'POST',  '/api/v1/siat/anulacion',                        'SIAT', 'Anula una factura válida en el SIN Bolivia')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_anular;
    IF r_siat_anular IS NULL THEN SELECT id INTO r_siat_anular FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/anulacion'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Revertir anulación SIAT',      'POST',  '/api/v1/siat/anulacion/revertir',               'SIAT', 'Revierte la anulación de una factura en el SIN')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_revertir;
    IF r_siat_revertir IS NULL THEN SELECT id INTO r_siat_revertir FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/anulacion/revertir'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Listar eventos significativos','GET',   '/api/v1/siat/eventos/{configId}',               'SIAT', 'Lista eventos registrados para una configuración')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_evt_list;
    IF r_siat_evt_list IS NULL THEN SELECT id INTO r_siat_evt_list FROM resources WHERE http_method='GET'   AND endpoint='/api/v1/siat/eventos/{configId}'; END IF;

    INSERT INTO resources (name, http_method, endpoint, module, description)
        VALUES ('Registrar evento significativo','POST', '/api/v1/siat/eventos',                          'SIAT', 'Registra un evento significativo en el SIN')
        ON CONFLICT (http_method, endpoint) DO NOTHING RETURNING id INTO r_siat_evt_create;
    IF r_siat_evt_create IS NULL THEN SELECT id INTO r_siat_evt_create FROM resources WHERE http_method='POST'  AND endpoint='/api/v1/siat/eventos'; END IF;


-- =============================================================================
-- 2. PERFILES
-- =============================================================================

    INSERT INTO profiles (name, description, active)
        VALUES ('ADMINISTRADOR', 'Acceso total al sistema. Gestiona usuarios, configuración y tenants.', TRUE)
        ON CONFLICT (name) DO NOTHING
        RETURNING id INTO p_admin;
    IF p_admin IS NULL THEN SELECT id INTO p_admin FROM profiles WHERE name = 'ADMINISTRADOR'; END IF;

    INSERT INTO profiles (name, description, active)
        VALUES ('OPERADOR', 'Gestiona la operación: flota, rutas, horarios, pasajes y encomiendas. Supervisa finanzas y aprueba reembolsos.', TRUE)
        ON CONFLICT (name) DO NOTHING
        RETURNING id INTO p_oper;
    IF p_oper IS NULL THEN SELECT id INTO p_oper FROM profiles WHERE name = 'OPERADOR'; END IF;

    INSERT INTO profiles (name, description, active)
        VALUES ('CAJERO', 'Vende pasajes, gestiona encomiendas, opera la caja y emite facturas.', TRUE)
        ON CONFLICT (name) DO NOTHING
        RETURNING id INTO p_cajero;
    IF p_cajero IS NULL THEN SELECT id INTO p_cajero FROM profiles WHERE name = 'CAJERO'; END IF;


-- =============================================================================
-- 3. ASIGNACION DE RECURSOS A PERFILES
-- =============================================================================

    -- ──────────────────────────────────────────────────────────────────────────
    -- ADMINISTRADOR: acceso total (93 recursos)
    -- ──────────────────────────────────────────────────────────────────────────
    INSERT INTO profile_resources (profile_id, resource_id) VALUES
        -- Usuarios
        (p_admin, r_users_list),       (p_admin, r_users_get),
        (p_admin, r_users_create),     (p_admin, r_users_update),     (p_admin, r_users_delete),
        (p_admin, r_profiles_list),    (p_admin, r_profiles_get),
        (p_admin, r_profiles_create),  (p_admin, r_profiles_update),
        (p_admin, r_profiles_res_add), (p_admin, r_profiles_res_rem), (p_admin, r_profiles_delete),
        (p_admin, r_resources_list),   (p_admin, r_resources_get),
        (p_admin, r_resources_create), (p_admin, r_resources_update), (p_admin, r_resources_delete),
        -- Operacion
        (p_admin, r_fleet_list),    (p_admin, r_fleet_get),    (p_admin, r_fleet_create),   (p_admin, r_fleet_update),   (p_admin, r_fleet_delete),
        (p_admin, r_buses_list),    (p_admin, r_buses_get),    (p_admin, r_buses_create),   (p_admin, r_buses_update),   (p_admin, r_buses_delete),
        (p_admin, r_drivers_list),  (p_admin, r_drivers_get),  (p_admin, r_drivers_create), (p_admin, r_drivers_update), (p_admin, r_drivers_delete),
        (p_admin, r_routes_list),   (p_admin, r_routes_get),   (p_admin, r_routes_create),  (p_admin, r_routes_update),  (p_admin, r_routes_delete),
        (p_admin, r_loc_list),      (p_admin, r_loc_get),      (p_admin, r_loc_create),     (p_admin, r_loc_update),     (p_admin, r_loc_delete),
        (p_admin, r_sched_list),    (p_admin, r_sched_get),    (p_admin, r_sched_by_route),
        (p_admin, r_sched_create),  (p_admin, r_sched_update), (p_admin, r_sched_delete),
        (p_admin, r_cust_list),     (p_admin, r_cust_get),     (p_admin, r_cust_by_doc),
        (p_admin, r_cust_create),   (p_admin, r_cust_update),  (p_admin, r_cust_delete),
        -- Pasajes
        (p_admin, r_tick_list),      (p_admin, r_tick_get),       (p_admin, r_tick_by_code),
        (p_admin, r_tick_create),    (p_admin, r_tick_bulk),
        (p_admin, r_tick_upd_cust),  (p_admin, r_tick_upd_seat), (p_admin, r_tick_cancel),
        (p_admin, r_seats_map),      (p_admin, r_seats_count),
        (p_admin, r_res_create),     (p_admin, r_res_confirm),    (p_admin, r_res_cancel),
        (p_admin, r_resch_list),     (p_admin, r_resch_create),
        (p_admin, r_refund_list),    (p_admin, r_refund_create),  (p_admin, r_refund_approve), (p_admin, r_refund_reject),
        -- Encomiendas
        (p_admin, r_parc_list),      (p_admin, r_parc_by_status), (p_admin, r_parc_get),
        (p_admin, r_parc_by_track),  (p_admin, r_parc_track_id),  (p_admin, r_parc_track_hist),
        (p_admin, r_parc_create),    (p_admin, r_parc_upd_status),
        -- Finanzas
        (p_admin, r_cash_list),   (p_admin, r_cash_get),    (p_admin, r_cash_summary),
        (p_admin, r_cash_open),   (p_admin, r_cash_close),
        (p_admin, r_txn_by_reg),  (p_admin, r_txn_create),
        (p_admin, r_inv_list),    (p_admin, r_inv_get),     (p_admin, r_inv_create),   (p_admin, r_inv_cancel),
        (p_admin, r_param_list),  (p_admin, r_param_get),   (p_admin, r_param_create), (p_admin, r_param_update),
        -- Tenants
        (p_admin, r_ten_list),    (p_admin, r_ten_get),     (p_admin, r_ten_by_schema),
        (p_admin, r_ten_create),  (p_admin, r_ten_update),  (p_admin, r_ten_status),
        -- Auditoria
        (p_admin, r_audit_list),
        -- SIAT (acceso total)
        (p_admin, r_siat_cfg_list),    (p_admin, r_siat_cfg_get),     (p_admin, r_siat_cfg_create),
        (p_admin, r_siat_cfg_update),  (p_admin, r_siat_cfg_toggle),
        (p_admin, r_siat_cuis_obtain), (p_admin, r_siat_cuis_vigente),
        (p_admin, r_siat_cufd_obtain), (p_admin, r_siat_cufd_vigente),
        (p_admin, r_siat_cat_list),    (p_admin, r_siat_cat_sync),
        (p_admin, r_siat_emit_get),    (p_admin, r_siat_emit_by_inv), (p_admin, r_siat_emit_by_cuf),
        (p_admin, r_siat_emit_estado), (p_admin, r_siat_emit_indiv),
        (p_admin, r_siat_emit_paquete),(p_admin, r_siat_emit_paq_val),
        (p_admin, r_siat_emit_masiva), (p_admin, r_siat_emit_mas_val),
        (p_admin, r_siat_anular),      (p_admin, r_siat_revertir),
        (p_admin, r_siat_evt_list),    (p_admin, r_siat_evt_create)
    ON CONFLICT DO NOTHING;

    -- ──────────────────────────────────────────────────────────────────────────
    -- OPERADOR: gestión operativa completa + supervisión financiera
    --   NO: gestión de usuarios/perfiles/recursos, tenants
    --   SÍ: flota, rutas, horarios, clientes, pasajes, encomiendas
    --   SÍ: finanzas en modo lectura + aprobar/rechazar reembolsos
    --   SÍ: auditoría (lectura)
    -- ──────────────────────────────────────────────────────────────────────────
    INSERT INTO profile_resources (profile_id, resource_id) VALUES
        -- Operacion (CRUD completo)
        (p_oper, r_fleet_list),   (p_oper, r_fleet_get),   (p_oper, r_fleet_create),   (p_oper, r_fleet_update),   (p_oper, r_fleet_delete),
        (p_oper, r_buses_list),   (p_oper, r_buses_get),   (p_oper, r_buses_create),   (p_oper, r_buses_update),   (p_oper, r_buses_delete),
        (p_oper, r_drivers_list), (p_oper, r_drivers_get), (p_oper, r_drivers_create), (p_oper, r_drivers_update), (p_oper, r_drivers_delete),
        (p_oper, r_routes_list),  (p_oper, r_routes_get),  (p_oper, r_routes_create),  (p_oper, r_routes_update),  (p_oper, r_routes_delete),
        (p_oper, r_loc_list),     (p_oper, r_loc_get),     (p_oper, r_loc_create),     (p_oper, r_loc_update),     (p_oper, r_loc_delete),
        (p_oper, r_sched_list),   (p_oper, r_sched_get),   (p_oper, r_sched_by_route),
        (p_oper, r_sched_create), (p_oper, r_sched_update),(p_oper, r_sched_delete),
        (p_oper, r_cust_list),    (p_oper, r_cust_get),    (p_oper, r_cust_by_doc),
        (p_oper, r_cust_create),  (p_oper, r_cust_update), (p_oper, r_cust_delete),
        -- Pasajes (completo, incluye aprobar/rechazar reembolsos)
        (p_oper, r_tick_list),     (p_oper, r_tick_get),      (p_oper, r_tick_by_code),
        (p_oper, r_tick_create),   (p_oper, r_tick_bulk),
        (p_oper, r_tick_upd_cust), (p_oper, r_tick_upd_seat), (p_oper, r_tick_cancel),
        (p_oper, r_seats_map),     (p_oper, r_seats_count),
        (p_oper, r_res_create),    (p_oper, r_res_confirm),   (p_oper, r_res_cancel),
        (p_oper, r_resch_list),    (p_oper, r_resch_create),
        (p_oper, r_refund_list),   (p_oper, r_refund_create), (p_oper, r_refund_approve), (p_oper, r_refund_reject),
        -- Encomiendas (completo)
        (p_oper, r_parc_list),     (p_oper, r_parc_by_status),(p_oper, r_parc_get),
        (p_oper, r_parc_by_track), (p_oper, r_parc_track_id), (p_oper, r_parc_track_hist),
        (p_oper, r_parc_create),   (p_oper, r_parc_upd_status),
        -- Finanzas (solo lectura + parámetros)
        (p_oper, r_cash_list),  (p_oper, r_cash_get),   (p_oper, r_cash_summary),
        (p_oper, r_txn_by_reg),
        (p_oper, r_inv_list),   (p_oper, r_inv_get),
        (p_oper, r_param_list), (p_oper, r_param_get),
        -- Auditoria
        (p_oper, r_audit_list),
        -- SIAT (operación completa: sin gestión de config)
        (p_oper, r_siat_cfg_list),    (p_oper, r_siat_cfg_get),
        (p_oper, r_siat_cuis_obtain), (p_oper, r_siat_cuis_vigente),
        (p_oper, r_siat_cufd_obtain), (p_oper, r_siat_cufd_vigente),
        (p_oper, r_siat_cat_list),    (p_oper, r_siat_cat_sync),
        (p_oper, r_siat_emit_get),    (p_oper, r_siat_emit_by_inv),  (p_oper, r_siat_emit_by_cuf),
        (p_oper, r_siat_emit_estado), (p_oper, r_siat_emit_indiv),
        (p_oper, r_siat_emit_paquete),(p_oper, r_siat_emit_paq_val),
        (p_oper, r_siat_emit_masiva), (p_oper, r_siat_emit_mas_val),
        (p_oper, r_siat_anular),      (p_oper, r_siat_revertir),
        (p_oper, r_siat_evt_list),    (p_oper, r_siat_evt_create)
    ON CONFLICT DO NOTHING;

    -- ──────────────────────────────────────────────────────────────────────────
    -- CAJERO: venta en ventanilla + caja + encomiendas
    --   NO: gestión de flota, conductores, rutas, horarios (solo consulta)
    --   NO: gestión de usuarios/perfiles, tenants, auditoría
    --   NO: aprobar/rechazar reembolsos (eso es del Operador)
    --   SÍ: clientes (CRUD), pasajes (completo), reservas, recambios
    --   SÍ: solicitar reembolso (no aprobarlo)
    --   SÍ: encomiendas (registrar y seguimiento)
    --   SÍ: caja (abrir, cerrar, transacciones, facturas)
    --   SÍ: parámetros (solo lectura, para consultar precios/tasas)
    -- ──────────────────────────────────────────────────────────────────────────
    INSERT INTO profile_resources (profile_id, resource_id) VALUES
        -- Operacion: flotas (solo lectura) + horarios (solo lectura) + clientes (CRUD)
        (p_cajero, r_fleet_list),    (p_cajero, r_fleet_get),
        (p_cajero, r_sched_list),    (p_cajero, r_sched_get),   (p_cajero, r_sched_by_route),
        (p_cajero, r_loc_list),      (p_cajero, r_loc_get),
        (p_cajero, r_cust_list),     (p_cajero, r_cust_get),    (p_cajero, r_cust_by_doc),
        (p_cajero, r_cust_create),   (p_cajero, r_cust_update),
        -- Pasajes (venta completa, sin cancelar pasaje — requiere autorización de Operador)
        (p_cajero, r_tick_list),     (p_cajero, r_tick_get),    (p_cajero, r_tick_by_code),
        (p_cajero, r_tick_create),   (p_cajero, r_tick_bulk),
        (p_cajero, r_tick_upd_cust), (p_cajero, r_tick_upd_seat),
        (p_cajero, r_seats_map),     (p_cajero, r_seats_count),
        (p_cajero, r_res_create),    (p_cajero, r_res_confirm), (p_cajero, r_res_cancel),
        (p_cajero, r_resch_list),    (p_cajero, r_resch_create),
        (p_cajero, r_refund_list),   (p_cajero, r_refund_create),
        -- Encomiendas (registrar y consultar)
        (p_cajero, r_parc_list),     (p_cajero, r_parc_by_status),(p_cajero, r_parc_get),
        (p_cajero, r_parc_by_track), (p_cajero, r_parc_track_id), (p_cajero, r_parc_track_hist),
        (p_cajero, r_parc_create),   (p_cajero, r_parc_upd_status),
        -- Finanzas (caja completa + facturas)
        (p_cajero, r_cash_list),  (p_cajero, r_cash_get),  (p_cajero, r_cash_summary),
        (p_cajero, r_cash_open),  (p_cajero, r_cash_close),
        (p_cajero, r_txn_by_reg),(p_cajero, r_txn_create),
        (p_cajero, r_inv_list),  (p_cajero, r_inv_get),   (p_cajero, r_inv_create),  (p_cajero, r_inv_cancel),
        (p_cajero, r_param_list),(p_cajero, r_param_get),
        -- SIAT (emisión individual + consultas; sin gestión de config ni procesos batch)
        (p_cajero, r_siat_cfg_list),    (p_cajero, r_siat_cfg_get),
        (p_cajero, r_siat_cuis_vigente),(p_cajero, r_siat_cufd_vigente),
        (p_cajero, r_siat_cat_list),
        (p_cajero, r_siat_emit_get),    (p_cajero, r_siat_emit_by_inv),(p_cajero, r_siat_emit_by_cuf),
        (p_cajero, r_siat_emit_estado), (p_cajero, r_siat_emit_indiv),
        (p_cajero, r_siat_evt_list)
    ON CONFLICT DO NOTHING;

END $$;
