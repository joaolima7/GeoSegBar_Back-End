#!/bin/bash

echo "=== ANÁLISE COMPLETA DE RESULTADOS ==="
echo "Data: $(date)"
echo "==============================================="

analyze_file() {
    local file=$1
    local description=$2
    
    echo ""
    echo "--- $description ---"
    
    if [ -f "$file" ]; then
        if grep -q "Complete requests" "$file"; then
            echo "✅ Teste executado com sucesso"
            echo "RPS: $(grep 'Requests per second' $file | awk '{print $4}' 2>/dev/null || echo 'N/A')"
            echo "Tempo médio: $(grep 'Time per request' $file | head -1 | awk '{print $4}' 2>/dev/null || echo 'N/A') ms"
            echo "Falhas: $(grep 'Failed requests' $file | awk '{print $3}' 2>/dev/null || echo '0')"
            echo "Total: $(grep 'Complete requests' $file | awk '{print $3}' 2>/dev/null || echo 'N/A')"
            
            if grep -q "50%" "$file"; then
                echo "50%: $(grep '50%' $file | awk '{print $2}' 2>/dev/null || echo 'N/A') ms"
                echo "95%: $(grep '95%' $file | awk '{print $2}' 2>/dev/null || echo 'N/A') ms"
                echo "99%: $(grep '99%' $file | awk '{print $2}' 2>/dev/null || echo 'N/A') ms"
            fi
        else
            echo "❌ Teste falhou"
            if grep -q "timeout" "$file"; then
                echo "Motivo: Timeout"
            elif grep -q "Connection refused" "$file"; then
                echo "Motivo: Conexão recusada"
            else
                echo "Motivo: Erro desconhecido"
            fi
        fi
    else
        echo "❌ Arquivo não encontrado: $file"
    fi
}

echo ""
echo "🔵 CHECKLIST RESPONSES"
echo "======================="
cd bench/checklists_responses 2>/dev/null || echo "Diretório checklists_responses não encontrado"
for file in *.txt; do
    if [ -f "$file" ]; then
        analyze_file "$file" "Checklist: $(basename $file .txt)"
    fi
done

echo ""
echo "🟡 INSTRUMENTS"
echo "=============="
cd ../instruments 2>/dev/null || echo "Diretório instruments não encontrado"
for file in *.txt; do
    if [ -f "$file" ]; then
        analyze_file "$file" "Instrument: $(basename $file .txt)"
    fi
done

echo ""
echo "🟢 READINGS"
echo "==========="
cd ../readings 2>/dev/null || echo "Diretório readings não encontrado"
for file in *.txt; do
    if [ -f "$file" ]; then
        analyze_file "$file" "Reading: $(basename $file .txt)"
    fi
done

echo ""
echo "=== ANÁLISE COMPLETA FINALIZADA ==="