#!/bin/bash

# progressive_test.sh
echo "=== TESTE PROGRESSIVO ==="
echo "Data: $(date)"
echo "==============================================="

BASE_URL="http://geometrisa-prod.com.br:9090/checklist-responses"
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJHZW9TZWdCYXIiLCJzdWIiOiJqY2dhbTNyNjlAZ21haWwuY29tIiwiZXhwIjoxNzUyOTkyMDAxfQ.Dzq2DWS5-ZsiIJl1qEZC7__zv6eMsF1kxPn4Zh5uwds"

# Teste com diferentes cargas
echo "Testando endpoint mais crítico com diferentes cargas..."

ENDPOINT="/dam/3/paged?page=0&size=10"

# Teste com 1 usuário (baseline)
echo "=== TESTE COM 1 USUÁRIO ==="
ab -n 10 -c 1 -s 30 \
   -H "Authorization: Bearer $JWT_TOKEN" \
   -H "Content-Type: application/json" \
   "$BASE_URL$ENDPOINT" > test_1user.txt 2>&1

echo "1 usuário - RPS: $(grep 'Requests per second' test_1user.txt | awk '{print $4}' 2>/dev/null || echo 'N/A')"
echo "1 usuário - Tempo médio: $(grep 'Time per request' test_1user.txt | head -1 | awk '{print $4}' 2>/dev/null || echo 'N/A')ms"

# Teste com 2 usuários
echo "=== TESTE COM 2 USUÁRIOS ==="
ab -n 20 -c 2 -s 30 \
   -H "Authorization: Bearer $JWT_TOKEN" \
   -H "Content-Type: application/json" \
   "$BASE_URL$ENDPOINT" > test_2users.txt 2>&1

echo "2 usuários - RPS: $(grep 'Requests per second' test_2users.txt | awk '{print $4}' 2>/dev/null || echo 'N/A')"
echo "2 usuários - Tempo médio: $(grep 'Time per request' test_2users.txt | head -1 | awk '{print $4}' 2>/dev/null || echo 'N/A')ms"

# Teste com 5 usuários
echo "=== TESTE COM 5 USUÁRIOS ==="
ab -n 25 -c 5 -s 30 \
   -H "Authorization: Bearer $JWT_TOKEN" \
   -H "Content-Type: application/json" \
   "$BASE_URL$ENDPOINT" > test_5users.txt 2>&1

echo "5 usuários - RPS: $(grep 'Requests per second' test_5users.txt | awk '{print $4}' 2>/dev/null || echo 'N/A')"
echo "5 usuários - Tempo médio: $(grep 'Time per request' test_5users.txt | head -1 | awk '{print $4}' 2>/dev/null || echo 'N/A')ms"

echo ""
echo "=== TESTE PROGRESSIVO FINALIZADO ==="