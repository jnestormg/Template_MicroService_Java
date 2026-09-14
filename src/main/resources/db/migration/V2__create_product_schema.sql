-- Tabla de productos (modulo de negocio de ejemplo)
CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120)  NOT NULL,
    description TEXT,
    price       NUMERIC(12, 2) NOT NULL,
    available   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE INDEX idx_products_name ON products (name);