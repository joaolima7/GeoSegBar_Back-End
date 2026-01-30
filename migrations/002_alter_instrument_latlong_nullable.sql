-- Migration: Tornar latitude e longitude opcionais no InstrumentEntity
-- Data: 2026-01-29

-- Alterar latitude para nullable
ALTER TABLE instrument ALTER COLUMN latitude DROP NOT NULL;

-- Alterar longitude para nullable  
ALTER TABLE instrument ALTER COLUMN longitude DROP NOT NULL;

-- Verificar alteração
SELECT column_name, is_nullable, data_type 
FROM information_schema.columns 
WHERE table_name = 'instrument' 
  AND column_name IN ('latitude', 'longitude');
