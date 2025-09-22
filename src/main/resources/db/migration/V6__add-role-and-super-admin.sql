INSERT INTO usuario_admin (id, nome, email, senha, role, status)
VALUES (
    UUID_TO_BIN(UUID()),
    'Super Admin',
    'super@fleet.com',
    '$2a$10$BQZ2wu7b/UqaVa0UAMeIxepTD/5jL5AGZtCYeyRrMTteiXBchfBBS', -- Placeholder "superadmin123"
    'SUPER_ADMIN',
    'ATIVO'
);