-- Datos iniciales: permisos, roles, usuario admin
INSERT INTO permissions (name) VALUES
    ('PRODUCT:CREATE'),
    ('PRODUCT:READ'),
    ('PRODUCT:UPDATE'),
    ('PRODUCT:DELETE'),
    ('USER:READ'),
    ('USER:UPDATE');

INSERT INTO roles (name) VALUES ('ADMIN'), ('USER');

-- ADMIN: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN';

-- USER: lectura y creacion de productos, lectura de usuarios
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'USER'
  AND p.name IN ('PRODUCT:READ', 'PRODUCT:CREATE', 'PRODUCT:UPDATE', 'USER:READ');

-- Usuario admin inicial: username = admin, password = admin123
-- (hash bcrypt $2y$ cost 10)
INSERT INTO users (username, email, password, enabled)
VALUES ('admin', 'admin@api.local',
        '$2y$10$pPVnyAQcsNaSnMqdBi.ajONHSTtuFQiB3MzBfo4WI6sRWp/YTSoAy',
        TRUE);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.username = 'admin' AND r.name = 'ADMIN';