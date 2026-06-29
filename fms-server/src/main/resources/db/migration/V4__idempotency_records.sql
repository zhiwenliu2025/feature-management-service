CREATE TABLE idempotency_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     VARCHAR(128) NOT NULL,
    operation_key       VARCHAR(512) NOT NULL,
    response_status     INT NOT NULL,
    response_body       JSONB NOT NULL,
    response_headers    JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_idempotency_key_operation UNIQUE (idempotency_key, operation_key)
);

CREATE INDEX idx_idempotency_records_expires_at ON idempotency_records (expires_at);
