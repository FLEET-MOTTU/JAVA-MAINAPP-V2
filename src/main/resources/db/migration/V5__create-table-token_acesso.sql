CREATE TABLE token_acesso (
    id BINARY(16) NOT NULL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    funcionario_id BINARY(16) NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    dispositivo_info VARCHAR(255),
    twilio_message_sid VARCHAR(255) NULL UNIQUE,
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id) ON DELETE CASCADE
);