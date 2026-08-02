CREATE TABLE rolusu (
    login VARCHAR(30) NOT NULL,
    codr INTEGER NOT NULL,
    fecha_asignacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_rolusu PRIMARY KEY (login, codr),
    CONSTRAINT fk_rolusu_login
        FOREIGN KEY (login)
        REFERENCES usuarios (login)
        ON DELETE CASCADE,
    CONSTRAINT fk_rolusu_codr
        FOREIGN KEY (codr)
        REFERENCES roles (codr)
        ON DELETE RESTRICT
);
