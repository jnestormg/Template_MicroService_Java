-- Permiso para crear usuarios (asignado al rol ADMIN)
INSERT INTO permissions (name) VALUES ('USER:CREATE');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name = 'USER:CREATE';