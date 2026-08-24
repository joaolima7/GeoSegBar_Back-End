-- ============================================================================
-- V1 — Tabela de token de definição de senha (primeiro acesso por link)
-- ============================================================================
-- Suporta o fluxo que substituiu o envio da senha temporária em texto plano no
-- e-mail de boas-vindas.
--
-- Condicional de propósito: em banco novo o Flyway roda antes do Hibernate, e
-- a tabela "users" ainda não existe. Nesse caso a migração não faz nada e o
-- ddl-auto cria tudo a partir das entidades, já no formato correto.
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'users'
    ) THEN
        RAISE NOTICE 'V1: tabela "users" ainda não existe (banco novo) — nada a fazer.';
        RETURN;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'password_setup_token'
    ) THEN
        RAISE NOTICE 'V1: tabela "password_setup_token" já existe — nada a fazer.';
        RETURN;
    END IF;

    CREATE TABLE password_setup_token (
        id          BIGSERIAL    PRIMARY KEY,
        token       VARCHAR(64)  NOT NULL,
        user_id     BIGINT       NOT NULL,
        expiry_date TIMESTAMP(6) NOT NULL,
        used        BOOLEAN      NOT NULL DEFAULT FALSE,
        used_at     TIMESTAMP(6),
        created_at  TIMESTAMP(6) NOT NULL,
        CONSTRAINT uk_password_setup_token_token UNIQUE (token),
        CONSTRAINT fk_password_setup_token_user  FOREIGN KEY (user_id) REFERENCES users (id)
    );

    CREATE INDEX idx_password_setup_token_user_id ON password_setup_token (user_id);
    CREATE INDEX idx_password_setup_token_expiry  ON password_setup_token (expiry_date);

    RAISE NOTICE 'V1: tabela "password_setup_token" criada.';
END $$;
