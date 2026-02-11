-- Migration: Tornar campo phone opcional na tabela client
-- Data: 2025-02-11
-- Descrição: Remove a constraint NOT NULL do campo phone

-- Verifica se a coluna existe e altera para permitir NULL
DO $$ 
BEGIN
    -- Altera a coluna phone para permitir NULL
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'client' 
        AND column_name = 'phone'
        AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE client ALTER COLUMN phone DROP NOT NULL;
        RAISE NOTICE 'Coluna phone da tabela client alterada para permitir NULL';
    ELSE
        RAISE NOTICE 'Coluna phone já permite NULL ou não existe';
    END IF;
END $$;
