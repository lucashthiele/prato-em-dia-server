-- Tabela do ShedLock (JDBC). Aditiva: a versão anterior da aplicação ignora a tabela,
-- como exige a regra das duas etapas (ADR 0003).
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL,
    locked_by  VARCHAR(255) NOT NULL,
    CONSTRAINT pk_shedlock PRIMARY KEY (name)
);
