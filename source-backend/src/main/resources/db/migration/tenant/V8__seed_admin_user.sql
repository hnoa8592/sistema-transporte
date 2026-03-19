-- =============================================================================
-- V8__seed_admin_user.sql
-- Corrige tenant_id nulos en usuarios existentes y crea el usuario admin por
-- defecto con perfil ADMINISTRADOR si aún no existe.
-- Password: Adminadmin.  (BCrypt)
-- =============================================================================

-- 1. Reparar tenant_id nulos en usuarios ya existentes
--    current_schema() devuelve el nombre del esquema del tenant actual
UPDATE users
SET tenant_id = current_schema()
WHERE tenant_id IS NULL
  AND deleted_at IS NULL;

-- 2. Crear usuario admin si no existe
DO $$
DECLARE
    v_profile_id UUID;
    v_schema     TEXT := current_schema();
BEGIN
    -- Obtener el perfil ADMINISTRADOR creado en V7
    SELECT id INTO v_profile_id
    FROM profiles
    WHERE name = 'ADMINISTRADOR'
      AND deleted_at IS NULL
    LIMIT 1;

    -- Insertar admin solo si no existe un usuario con ese username
    IF NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin') THEN
        INSERT INTO users (
            id, username, email, password,
            first_name, last_name,
            active, profile_id, tenant_id,
            created_at, updated_at, created_by
        ) VALUES (
            gen_random_uuid(),
            'admin',
            'admin@' || v_schema || '.local',
            '$2a$10$uwhLSYUPrNUZVZDWiBS33usFVP9HIs9FVSDZWgpqMiaeksCumHxt6',
            'Administrador',
            'Sistema',
            TRUE,
            v_profile_id,
            v_schema,
            NOW(), NOW(), 'system'
        );
        RAISE NOTICE 'Admin user created in schema: %', v_schema;
    ELSE
        -- Si el usuario ya existe pero tiene tenant_id nulo, corregirlo
        UPDATE users
        SET tenant_id  = v_schema,
            profile_id = COALESCE(profile_id, v_profile_id),
            updated_at = NOW()
        WHERE username = 'admin'
          AND tenant_id IS NULL;
        RAISE NOTICE 'Admin user already exists in schema: %', v_schema;
    END IF;
END $$;
