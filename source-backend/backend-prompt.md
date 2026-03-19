Eres un arquitecto de software senior especializado en Java 21 y Spring Boot 3.x.
Debes generar el backend completo de un sistema SaaS de transporte de buses  para Bolivia (moneda en Bolivianos) con las
siguientes especificaciones:

## STACK TÉCNICO
- Java 21 (records, sealed classes, pattern matching)
- Spring Boot 3.x
- Spring Security 6 + JWT (stateless)
- PostgreSQL 16
- Flyway (migraciones por esquema)
- MapStruct (DTOs)
- Lombok
- Maven
- Caffeine Cache
- OpenAPI/Swagger

## ARQUITECTURA
Modular Monolith con los siguientes módulos Maven:
- core (shared kernel: excepciones, utils, auditoría)
- security (auth, JWT, multitenancy)
- tenants (gestión de empresas/tenants SaaS, schema público)
- usuarios (usuarios, perfiles, recursos/permisos)
- operacion (flota, buses, choferes, rutas, horarios, terminales, clientes, ubicaciones)
- pasajes (venta, reservas, croquis de asientos, reprogramación, devolución)
- encomiendas (recepción, transporte, entrega, tracking, estados)
- finanzas (cajas, facturación, reportes, parámetros)

## MULTITENANCY
- Estrategia: Multi-Tenant por ESQUEMA de PostgreSQL
- Resolver el tenant desde el JWT (claim: tenantId)
- Configurar Hibernate con schema dinámico por request
- Flyway aplica migraciones por esquema al crear una empresa nueva (TenantProvisioningService)
- Esquema público solo para tabla de empresas/tenants y refresh_tokens
- El módulo `tenants` gestiona el ciclo de vida: crear tenant provisiona automáticamente su schema

## SEGURIDAD
- JWT con claims: userId, tenantId, roles, permisos
- Spring Security con filtros por tenant
- Control de acceso basado en RECURSOS (endpoints registrados en BD)
- Refresh token con rotación
- Auditoría automática (createdBy, updatedBy, tenantId) con @EntityListeners

## MÓDULO AUDITORIA
Registro transversal de eventos de negocio usando AOP. No invade la lógica de negocio existente.

Entidades:
- AuditLog: id (UUID), tenantId, userId, username, action (AuditAction), entityType, entityId,
  httpMethod, endpoint, ipAddress, description, status (AuditStatus), errorMessage, createdAt
  * NO extiende BaseEntity (es el registro en sí)
  * @Table(name = "audit_logs") — schema del tenant
  * @PrePersist → createdAt = LocalDateTime.now()

Enums:
- AuditAction: CREATE, UPDATE, DELETE, STATUS_CHANGE, CANCEL, APPROVE, REJECT, RESCHEDULE, OPEN, CLOSE
- AuditStatus: SUCCESS, FAILURE

Componentes:
- `@Auditable`: anotación en métodos de servicio con campos action, entityType, description
- AuditAspect: @Around intercepta métodos con @Auditable; omite si TenantContext es null (login);
  extrae entityId: primero del retorno vía result.id() (reflection), luego del primer arg UUID
- AuditService: log() con Propagation.REQUIRES_NEW (aislado del tx principal); métodos de consulta paginados
- UserContext: ThreadLocal<String> para userId, inicializado en JwtAuthenticationFilter

Endpoints REST `/api/v1/audit`:
- GET / → listar logs paginado; filtros opcionales: entityType, username, from, to (ISO datetime)

Servicios auditados (anotación @Auditable en los métodos write):
- TenantService: create(CREATE), update(UPDATE), updateStatus(STATUS_CHANGE)
- UserService: create(CREATE), update(UPDATE), delete(DELETE)
- TicketService: create(CREATE), createBulk(CREATE), changeSeat(UPDATE), cancel(CANCEL)
- ReservationService: create(CREATE), confirm(STATUS_CHANGE), cancel(CANCEL)
- RescheduleService: reschedule(RESCHEDULE)
- RefundService: requestRefund(CREATE), approve(APPROVE), reject(REJECT)
- ParcelService: create(CREATE), updateStatus(STATUS_CHANGE)
- CashRegisterService: open(OPEN), close(CLOSE)
- InvoiceService: create(CREATE), cancel(CANCEL)

Migración Flyway: tenant/V6__auditoria.sql — tabla audit_logs + 4 índices

## PERFILES Y PERMISOS POR DEFECTO
Seed de datos ejecutado en la migración tenant/V7__seed_roles_resources.sql (PL/pgSQL con DO $$).
Registra todos los recursos (endpoints) del sistema y crea 3 perfiles base asignados a cada tenant.

### Recursos del sistema
93 recursos registrados en la tabla `resources`, agrupados por módulo:
- USUARIOS (17): users CRUD, profiles CRUD + gestión de recursos, resources CRUD
- OPERACION (27): buses, drivers, routes, schedules (+ por ruta), customers (+ por documento) — CRUD completo
- PASAJES (19): tickets (venta individual, venta múltiple/bulk, cambio asiento, cambio pasajero, cancelar), seat-maps, reservations, reschedules, refunds (solicitar, aprobar, rechazar)
- ENCOMIENDAS (8): parcels (CRUD + por estado + tracking por ID y por código)
- FINANZAS (15): cash-registers (abrir, cerrar, resumen), cash-transactions, invoices (crear, anular), parameters CRUD
- TENANTS (6): CRUD + por schema + cambiar status
- AUDITORIA (1): GET /api/v1/audit

### Perfiles por defecto

**ADMINISTRADOR** — Acceso total (93 recursos)
- Todos los módulos sin restricción
- Único perfil que puede gestionar usuarios, perfiles, recursos y tenants
- Acceso completo a auditoría y parámetros del sistema

**OPERADOR** — Gestión operativa + supervisión (69 recursos)
- OPERACION: CRUD completo (buses, conductores, rutas, horarios, clientes)
- PASAJES: completo incluyendo aprobar/rechazar reembolsos y venta múltiple
- ENCOMIENDAS: completo
- FINANZAS: solo lectura (cajas, transacciones, facturas, parámetros)
- AUDITORIA: lectura
- NO: gestión de usuarios/perfiles/recursos, tenants, abrir/cerrar caja

**CAJERO** — Venta en ventanilla + caja (47 recursos)
- OPERACION: horarios (solo lectura) + clientes (CRUD sin eliminar)
- PASAJES: venta completa individual y múltiple (tickets, asientos, reservas, recambios) + solicitar reembolso (no aprobar/rechazar)
- ENCOMIENDAS: completo
- FINANZAS: caja completa (abrir, cerrar, transacciones, facturas) + parámetros (solo lectura)
- NO: gestión de flota/rutas/horarios, usuarios, tenants, auditoría

### Convenciones del script
- Idempotente: `ON CONFLICT DO NOTHING` en todos los inserts
- Ejecutado automáticamente por Flyway al provisionar cada nuevo tenant
- Implementado con bloque PL/pgSQL `DO $$` usando variables UUID para gestionar relaciones profile_resources

## MÓDULO TENANTS
Gestión de empresas/tenants del SaaS. Opera sobre el schema `public` (cross-tenant).
Entidades:
- Tenant: id (UUID), name, schemaName, contactEmail, contactPhone, address,
  active, plan (TenantPlan), status (TenantStatus), createdAt, updatedAt
  * No extiende BaseEntity (datos cross-tenant)
  * @Table(schema = "public")
  * @PrePersist / @PreUpdate para timestamps manuales

Enums:
- TenantPlan: BASIC, STANDARD, PREMIUM
- TenantStatus: ACTIVE, INACTIVE, SUSPENDED

Servicios:
- TenantProvisioningService: CREATE SCHEMA IF NOT EXISTS + Flyway tenant migrations
  (classpath:db/migration/tenant) vía Java API con DataSource
- TenantService: CRUD + validación de unicidad (name, schemaName) +
  llamada a provisioning al crear; updateStatus solo cambia el campo status

Endpoints REST `/api/v1/tenants`:
- GET    /               → listar todos (paginado)
- GET    /{id}           → obtener por UUID
- GET    /schema/{name}  → obtener por schemaName
- POST   /               → crear tenant + provisionar schema
- PUT    /{id}           → actualizar datos del tenant
- PATCH  /{id}/status    → cambiar estado (ACTIVE/INACTIVE/SUSPENDED)

Migraciones Flyway (schema público):
- V1__create_public_schema.sql: tablas tenants (con columna active) y refresh_tokens
- V2__add_tenant_plan_status.sql: agrega columnas plan VARCHAR(20) y status VARCHAR(20)

Validaciones:
- schemaName: patrón `^[a-z][a-z0-9_]{2,49}$` (solo minúsculas, dígitos, guión bajo; 3-50 chars)
- contactEmail: @Email @NotBlank
- name: @NotBlank @Size(max=200)

## MÓDULO USUARIOS
Entidades: User, Profile, Resource
- User: id, username, email, password (bcrypt), active, tenantId, profileId
- Profile: id, name, description, resources[]
- Resource: id, name, httpMethod, endpoint, module, active
- CRUD completo con paginación (Pageable)
- Asignación de recursos a perfiles
- El login devuelve access_token + refresh_token

## MÓDULO OPERACIÓN
Entidades y funcionalidades:
- Department/State (departamento o estado según país)
- Province/Locality (provincia o localidad)
- Fleet (flota): nombre, descripción, empresa
- Bus: placa, modelo, marca, año, fleetId, hasTwoFloors, totalSeats,
  seatsFirstFloor, seatsSecondFloor, active
- Driver (chofer): dni, nombre, apellido, licencia, categoría, vencimiento,
  teléfono, active
- Route (ruta): origen (locationId), destino (locationId), distanciaKm,
  duracionEstimada, active
- Schedule (horario): routeId, busId, driverId, departureTime, arrivalTime,
  frequency, daysOfWeek[], active
- Terminal: nombre, ubicación, locationId
- TerminalLane (carril/puerta): terminalId, number, description, active (opcional)
- Customer (cliente): dni/ruc, nombre, apellido, email, teléfono, dirección

## MÓDULO PASAJES
Funcionalidades:
- SeatMap (croquis): generación dinámica basada en bus (1 o 2 pisos),
  estado por asiento (DISPONIBLE, RESERVADO, VENDIDO, BLOQUEADO)
  * Layout de 4 carriles: columnas A-B | pasillo | C-D (frontend)
- Ticket (pasaje): scheduleId, customerId, seatNumber, floor, price,
  status, saleType (VENTANILLA/ONLINE), employeeId
- Sale (venta en ventanilla):
  * Selección múltiple de asientos en la misma pantalla
  * Asignación de un cliente distinto por asiento (búsqueda por documento o registro nuevo)
  * Venta individual: POST /api/v1/tickets
  * Venta múltiple (bulk): POST /api/v1/tickets/bulk — DTO BulkTicketRequest { List<TicketRequest> tickets }
    → Atómica: si algún asiento ya está ocupado, hace rollback de todos
    → Respuesta: List<TicketResponse>
  * Cambio de asiento antes de confirmar
  * Generación de recibo o factura (configurable por empresa)
- Reservation: ticketId, expiresAt, status (PENDING, CONFIRMED, CANCELLED, EXPIRED)
  * Endpoints REST `/api/v1/reservations`:
    - GET  /               → listar paginado; filtro opcional: status (ReservationStatus)
    - POST /               → crear reserva (ticketId, notes)
    - POST /{id}/confirm   → confirmar → asiento pasa a SOLD
    - DELETE /{id}         → cancelar → asiento vuelve a AVAILABLE
  * Expiración automática configurable: app.reservation.expiry-minutes (default 30)
  * ReservationRepository: findAllByOrderByCreatedAtDesc, findByStatusOrderByCreatedAtDesc
- Reschedule (reprogramación): ticketId original, nuevo scheduleId, motivo, fee
- Refund (devolución): ticketId, motivo, retentionPercent (configurable),
  montoRetenido, montoDevuelto, status

## MÓDULO ENCOMIENDAS
Entidades:
- Parcel (encomienda): código, remitente (customerId), destinatario (customerId),
  scheduleId, descripción, peso, valorDeclarado, precio, status
- ParcelTracking: parcelId, status, location, timestamp, notes
- Estados: RECIBIDO, EN_TRANSITO, EN_DESTINO, ENTREGADO, DEVUELTO
- Generación de código de tracking único
- Notificación de cambio de estado (evento interno)

## MÓDULO FINANZAS
Funcionalidades:
- CashRegister (caja): empleadoId, openedAt, initialAmount, finalAmount, notes,
  status (OPEN, CLOSED) — un empleado puede tener solo una caja OPEN a la vez
  * Endpoints REST `/api/v1/cash-registers`:
    - GET  /                          → listar paginado (sort, direction)
    - GET  /{id}                      → obtener por ID
    - GET  /{id}/summary              → resumen: totalIncomes, totalExpenses, expectedFinalAmount
    - GET  /employee/{employeeId}/open → caja abierta del empleado (data=null si no existe)
    - POST /open                      → abrir caja (employeeId, initialAmount, notes)
    - POST /{id}/close                → cerrar caja (finalAmount, notes)
  * Pruebas de integración: `CajaDiaIntegrationTest.java` — 13 tests ✅
    Cubre: verificar caja abierta, apertura, apertura duplicada, monto cero,
    ingreso/egreso/caja-cerrada, resumen financiero, listado paginado, cierre,
    cierre duplicado, cierre sin monto, flujo E2E completo, multi-empleado
- TransactionType enum: `INGRESO` / `EGRESO` (NO INCOME/EXPENSE)
- CashTransaction: cajaId, tipo (INGRESO/EGRESO), concepto, monto, referenceId,
  referenceType (TICKET, PARCEL, REFUND)
- Invoice (factura): número correlativo por empresa, customerId, items[], total,
  taxPercent, status (EMITIDA, ANULADA)
- Reports: ventas por ruta, por fecha, por empleado, por bus; encomiendas; cajas
- SystemParameter: empresa-specific, key-value con tipo (STRING, NUMBER, BOOLEAN, JSON)
  Ejemplos: RETENTION_PERCENT, INVOICE_PREFIX, TAX_PERCENT, RESERVATION_EXPIRY_MINUTES

## PATRONES Y CONVENCIONES
- Arquitectura en capas: Controller → Service → Repository (Spring Data JPA)
- DTOs con record de Java 21 para requests/responses
- Manejo global de excepciones con @RestControllerAdvice
- Respuesta estándar: ApiResponse<T> { success, message, data, timestamp }
- Paginación estándar: PageResponse<T> { content, page, size, totalElements, totalPages }
- Validaciones con Bean Validation (@Valid)
- Eventos de dominio con ApplicationEventPublisher para desacoplamiento entre módulos
- Cache con Caffeine para: rutas, horarios, parámetros de sistema
- Soft delete con campo active/deletedAt

## LO QUE DEBES GENERAR
1. Estructura completa del proyecto Maven (monolito modular, único pom.xml, packaging=jar)
2. Configuración de multitenancy (TenantContext, TenantFilter, HibernateConfig)
3. Configuración de Flyway multi-esquema (public + tenant por empresa)
4. Entidades JPA completas con auditoría (BaseEntity para entidades tenant, Tenant sin BaseEntity)
5. Repositorios Spring Data JPA
6. Servicios con lógica de negocio
7. Controllers REST con endpoints documentados con OpenAPI
8. DTOs (records Java 21)
9. Mappers con MapStruct
10. Migraciones Flyway (public: V1, V2; tenant: V1-V6 por módulo + V7 seed roles)
11. Security config completa (filtros JWT + tenant)
12. application.yml con todos los perfiles (dev, prod)
13. Manejo de excepciones global
14. Realizar pruebas unitarias y de integración para los módulos de PASAJES y ENCOMIENDAS
15. Módulo de auditoría transversal (AuditLog, @Auditable, AuditAspect, UserContext, V6 migration)
16. Seed de perfiles y permisos por defecto (V7__seed_roles_resources.sql):
    - 93 recursos del sistema registrados en tabla `resources`
    - 3 perfiles: ADMINISTRADOR (acceso total), OPERADOR (gestión operativa), CAJERO (venta + caja)
    - Script idempotente con PL/pgSQL DO $$ y ON CONFLICT DO NOTHING


Genera el código completo, sin omitir implementaciones. Comienza por la estructura
del proyecto y el módulo core/security, luego continúa módulo por módulo.