CREATE TABLE pateo (
    id BINARY(16) NOT NULL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    planta_baixa_url VARCHAR(255),
    planta_largura INT,
    planta_altura INT,
    gerenciado_por_id BINARY(16) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (gerenciado_por_id) REFERENCES usuario_admin(id)
);