CREATE TABLE pateo (
    id BINARY(16) NOT NULL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    planta_baixa_url VARCHAR(255),
    gerenciado_por_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (gerenciado_por_id) REFERENCES usuario_admin(id)
);