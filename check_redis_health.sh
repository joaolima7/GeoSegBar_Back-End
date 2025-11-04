#!/bin/bash

echo "🔍 Verificando saúde do Redis..."

# Verificar se container está rodando
if ! docker ps -q -f name=redis-prod | grep -q .; then
    echo "❌ Redis não está rodando!"
    exit 1
fi

# Verificar role
REDIS_ROLE=$(docker exec redis-prod redis-cli INFO replication | grep "role:" | cut -d: -f2 | tr -d '\r')

if [ "$REDIS_ROLE" != "master" ]; then
    echo "❌ PROBLEMA DETECTADO: Redis está em modo $REDIS_ROLE (esperado: master)"
    echo "🔧 Corrigindo automaticamente..."
    docker exec redis-prod redis-cli REPLICAOF NO ONE
    
    # Verificar novamente
    sleep 2
    NEW_ROLE=$(docker exec redis-prod redis-cli INFO replication | grep "role:" | cut -d: -f2 | tr -d '\r')
    
    if [ "$NEW_ROLE" == "master" ]; then
        echo "✅ Redis corrigido com sucesso! Agora está em modo master"
    else
        echo "❌ Falha ao corrigir Redis. Role atual: $NEW_ROLE"
        exit 1
    fi
else
    echo "✅ Redis está saudável e em modo master"
fi

# Verificar ping
if docker exec redis-prod redis-cli PING | grep -q "PONG"; then
    echo "✅ Redis respondendo a PING"
else
    echo "❌ Redis não está respondendo!"
    exit 1
fi

# Verificar memória
USED_MEMORY=$(docker exec redis-prod redis-cli INFO memory | grep "used_memory_human:" | cut -d: -f2 | tr -d '\r')
echo "📊 Memória usada: $USED_MEMORY"

echo "🎉 Verificação concluída com sucesso!"