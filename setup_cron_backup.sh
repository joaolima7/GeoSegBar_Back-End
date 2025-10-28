#!/bin/bash

set -e

echo "⏰ Configurando backup automático do banco de dados..."

# Diretório do projeto
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_SCRIPT="$PROJECT_DIR/backup_database_prod.sh"

# Verificar se o script existe
if [ ! -f "$BACKUP_SCRIPT" ]; then
    echo "❌ Script de backup não encontrado: $BACKUP_SCRIPT"
    exit 1
fi

# Tornar script executável
chmod +x "$BACKUP_SCRIPT"

# Configurar cron job (executar diariamente às 2h da manhã)
CRON_TIME="0 2 * * *"  # Minuto Hora Dia Mês DiaDaSemana
CRON_JOB="$CRON_TIME $BACKUP_SCRIPT >> $PROJECT_DIR/logs/cron_backup.log 2>&1"

# Verificar se já existe
if crontab -l 2>/dev/null | grep -q "$BACKUP_SCRIPT"; then
    echo "⚠️  Job de backup já está configurado no cron"
    echo "📋 Cron atual:"
    crontab -l | grep "$BACKUP_SCRIPT"
else
    # Adicionar ao cron
    (crontab -l 2>/dev/null; echo "$CRON_JOB") | crontab -
    echo "✅ Job de backup adicionado ao cron!"
    echo "⏰ Horário configurado: Diariamente às 2h da manhã"
fi

echo ""
echo "📋 Para verificar os jobs do cron:"
echo "   crontab -l"
echo ""
echo "📋 Para editar manualmente:"
echo "   crontab -e"
echo ""
echo "📋 Para remover o job:"
echo "   crontab -l | grep -v '$BACKUP_SCRIPT' | crontab -"
echo ""
echo "✅ Configuração concluída!"