CREATE TABLE zona (
    id BINARY(16) NOT NULL PRIMARY KEY,
    pateo_id BINARY(16) NOT NULL,
    criado_por_id BINARY(16) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    cor VARCHAR(7),
    coordenadas GEOMETRY NOT NULL SRID 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pateo_id) REFERENCES pateo(id),
    FOREIGN KEY (criado_por_id) REFERENCES usuario_admin(id),
    SPATIAL INDEX(coordenadas)
);