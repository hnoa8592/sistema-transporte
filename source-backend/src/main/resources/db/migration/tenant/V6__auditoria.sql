CREATE TABLE audit_logs (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(100) NOT NULL,
    user_id       VARCHAR(100),
    username      VARCHAR(200),
    action        VARCHAR(50)  NOT NULL,
    entity_type   VARCHAR(100),
    entity_id     VARCHAR(100),
    http_method   VARCHAR(10),
    endpoint      VARCHAR(500),
    ip_address    VARCHAR(50),
    description   VARCHAR(1000),
    status        VARCHAR(20)  NOT NULL,
    error_message TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_tenant_id   ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_username    ON audit_logs(username);
CREATE INDEX idx_audit_logs_created_at  ON audit_logs(created_at);
