ALTER TABLE personas
    ADD COLUMN creada_por_login VARCHAR(30);

ALTER TABLE personas
    ADD CONSTRAINT fk_personas_creada_por_usuario
        FOREIGN KEY (creada_por_login)
        REFERENCES usuarios (login)
        ON DELETE RESTRICT;

CREATE INDEX idx_personas_creada_por_login
    ON personas (creada_por_login);
