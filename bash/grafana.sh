#!/usr/bin/env bash
#
# Abre o Grafana de produção na sua máquina, por túnel SSH, e mostra o usuário
# e a senha do admin no terminal. Fica rodando até Ctrl+C, que fecha o túnel.
#
#   ./bash/grafana.sh            # porta local 3000 (ou a próxima livre)
#   ./bash/grafana.sh 3100       # outra porta local
#
# Variáveis opcionais:
#   GEOSEGBAR_SSH_HOST=geosegbar-vps   host do ~/.ssh/config
#   GRAFANA_NO_BROWSER=1               não abre o navegador
#
# Uma conexão SSH só (ControlMaster): o túnel e a leitura da senha usam a
# mesma autenticação. Nada é gravado em disco; a senha só passa pela memória
# deste script e pelo terminal. Ver docs/MONITORAMENTO.md, "Como chegar ao
# Grafana".

set -Eeuo pipefail

HOST="${GEOSEGBAR_SSH_HOST:-geosegbar-vps}"
PORTA_INICIAL="${1:-3000}"
PORTA_REMOTA=3000
ENV_REMOTO=/etc/geosegbar/monitoring/monitoring.env
SENHA_REMOTA=/etc/geosegbar/monitoring/secrets/grafana_admin_password

erro() { printf '❌ %s\n' "$*" >&2; exit 1; }
info() { printf '%s\n' "$*"; }

command -v ssh >/dev/null 2>&1 || erro "ssh não encontrado."
command -v curl >/dev/null 2>&1 || erro "curl não encontrado."
case "$PORTA_INICIAL" in
  '' | *[!0-9]*) erro "Porta inválida: $PORTA_INICIAL" ;;
esac

porta_ocupada() {
  # Algo já escuta nessa porta (IPv4 ou IPv6) se a conexão abre. O navegador
  # pode resolver localhost para ::1, então as duas famílias contam.
  (: </dev/tcp/127.0.0.1/"$1") 2>/dev/null || (: </dev/tcp/::1/"$1") 2>/dev/null
}

PORTA="$PORTA_INICIAL"
while porta_ocupada "$PORTA"; do
  PORTA=$((PORTA + 1))
  [ "$PORTA" -le $((PORTA_INICIAL + 20)) ] || erro "Nenhuma porta livre entre $PORTA_INICIAL e $((PORTA_INICIAL + 20))."
done
[ "$PORTA" = "$PORTA_INICIAL" ] || info "ℹ️  Porta $PORTA_INICIAL ocupada; usando $PORTA."

# Caminho curto: o macOS limita o socket a 104 caracteres.
SOCK_DIR="$(mktemp -d /tmp/gsb-grafana.XXXXXX)"
SOCK="$SOCK_DIR/ssh"

fechar() {
  ssh -S "$SOCK" -O exit "$HOST" >/dev/null 2>&1 || true
  rm -rf "$SOCK_DIR"
}
trap fechar EXIT
trap 'echo; info "🔒 Túnel fechado."; exit 0' INT TERM

# Sem endereço de bind, o ssh escuta só no loopback (127.0.0.1 e ::1).
info "🔐 Abrindo túnel para $HOST (localhost:$PORTA -> Grafana)..."
ssh -M -S "$SOCK" -f -N \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  -L "$PORTA:127.0.0.1:$PORTA_REMOTA" \
  "$HOST" || erro "Não consegui abrir o túnel. Teste: ssh $HOST true"

# Roda no servidor: usuário (do monitoring.env, sem aspas) na primeira linha,
# senha na segunda. Fica num heredoc com aspas para nada expandir aqui.
read -r -d '' SCRIPT_REMOTO <<'REMOTO' || true
u=$(sed -n 's/^GRAFANA_ADMIN_USER=//p' "$1" 2>/dev/null | tail -n 1 | tr -d "\"'")
printf '%s\n' "${u:-gsb-admin}"
cat "$2" 2>/dev/null || sudo -n cat "$2" 2>/dev/null
REMOTO

CREDENCIAIS="$(printf '%s\n' "$SCRIPT_REMOTO" | ssh -S "$SOCK" "$HOST" sh -s -- "$ENV_REMOTO" "$SENHA_REMOTA" 2>/dev/null || true)"
USUARIO="$(printf '%s\n' "$CREDENCIAIS" | sed -n 1p)"
SENHA="$(printf '%s\n' "$CREDENCIAIS" | sed -n 2p)"
unset CREDENCIAIS

URL="http://localhost:$PORTA"

saudavel=false
for _ in 1 2 3 4 5 6 7 8 9 10; do
  saude="$(curl -fsS --max-time 3 "$URL/api/health" 2>/dev/null || true)"
  case "$saude" in
    *'"database"'*'"ok"'*) saudavel=true; break ;;
  esac
  sleep 1
done
$saudavel || info "⚠️  O túnel abriu, mas o Grafana não respondeu em $URL/api/health. Veja: ssh $HOST 'docker ps --filter name=geosegbar-grafana'"

echo
info "📈 Grafana: $URL"
info "   usuário: ${USUARIO:-gsb-admin}"
if [ -n "$SENHA" ]; then
  info "   senha:   $SENHA"
  if command -v pbcopy >/dev/null 2>&1; then
    printf '%s' "$SENHA" | pbcopy
    info "   (senha copiada para a área de transferência)"
  fi
else
  info "   senha:   não consegui ler $SENHA_REMOTA no servidor (precisa de root)."
fi
unset SENHA
echo

# O cookie de sessão do Grafana é Secure (GRAFANA_COOKIE_SECURE=true). Chrome,
# Firefox e os derivados do Chromium aceitam isso em http://localhost; o
# Safari não, e o login volta para a tela inicial sem erro.
abrir_navegador() {
  if [ "$(uname -s)" = "Darwin" ]; then
    for app in "Google Chrome" "Firefox" "Brave Browser" "Microsoft Edge" "Arc" "Chromium"; do
      open -a "$app" "$URL" >/dev/null 2>&1 && return 0
    done
    info "⚠️  Não achei Chrome nem Firefox. No Safari o login não funciona (cookie Secure em http)."
    open "$URL"
  elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "$URL" >/dev/null 2>&1 &
  fi
}
[ "${GRAFANA_NO_BROWSER:-0}" = "1" ] || abrir_navegador || true

info "Túnel aberto. Ctrl+C para fechar."
while ssh -S "$SOCK" -O check "$HOST" >/dev/null 2>&1; do
  sleep 5
done
erro "A conexão SSH caiu. Rode o script de novo."
