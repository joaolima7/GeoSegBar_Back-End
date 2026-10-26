#!/usr/bin/env bash
#
# Publica o Grafana só dentro da tailnet, com HTTPS, sem abrir porta na internet.
# Rode como root no servidor de produção, uma vez. Interativo: o `tailscale up`
# imprime um link de login que você abre no navegador.
#
#   sudo bash bash/scripts/monitoring/setup_tailscale.sh
#
# O que faz:
#   - instala o tailscale pelo repositório oficial (RHEL/AlmaLinux 9);
#   - entra na tailnet SEM aceitar DNS da tailnet (--accept-dns=false): este
#     servidor é o DNS das zonas do cPanel, e o MagicDNS reescreveria o
#     /etc/resolv.conf;
#   - `tailscale serve` em 8443 → http://127.0.0.1:3000 (Grafana). Nunca
#     `tailscale funnel`, que publicaria na internet.
#
# Depois: ponha a URL impressa no fim em GRAFANA_ROOT_URL
# (/etc/geosegbar/monitoring/monitoring.env) e rode o deploy do monitoramento.

set -Eeuo pipefail

HOSTNAME_TS="${TS_HOSTNAME:-geosegbar-prod}"
SERVE_PORT="${TS_SERVE_PORT:-8443}"
GRAFANA_LOCAL="http://127.0.0.1:${GRAFANA_PORT:-3000}"

die() { echo "❌ $*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "Rode como root."
command -v dnf >/dev/null 2>&1 || die "Este script é para RHEL/AlmaLinux (dnf)."

if ! command -v tailscale >/dev/null 2>&1; then
  . /etc/os-release
  MAJOR="${VERSION_ID%%.*}"
  echo "📦 Instalando tailscale (repositório oficial, el${MAJOR})..."
  dnf config-manager --add-repo "https://pkgs.tailscale.com/stable/rhel/${MAJOR}/tailscale.repo"
  dnf install -y tailscale
fi

systemctl enable --now tailscaled

if ! tailscale status >/dev/null 2>&1; then
  echo "🔑 Entrando na tailnet. Abra o link abaixo e autorize o servidor:"
  tailscale up --hostname="$HOSTNAME_TS" --accept-dns=false --accept-routes=false --ssh=false
fi

echo "🌐 Publicando o Grafana só na tailnet (porta ${SERVE_PORT})..."
tailscale serve --bg --https="$SERVE_PORT" "$GRAFANA_LOCAL"
tailscale serve status

DNS_NAME="$(tailscale status --json | python3 -c 'import json,sys; print(json.load(sys.stdin)["Self"]["DNSName"].rstrip("."))' 2>/dev/null || true)"

echo ""
echo "✅ Pronto."
if [ -n "$DNS_NAME" ]; then
  echo "   URL do Grafana na tailnet: https://${DNS_NAME}:${SERVE_PORT}/"
  echo "   Ponha em /etc/geosegbar/monitoring/monitoring.env:"
  echo "     GRAFANA_ROOT_URL=https://${DNS_NAME}:${SERVE_PORT}/"
fi
echo ""
echo "Próximos passos (docs/MONITORAMENTO.md, \"Acesso pelo Tailscale\"):"
echo "  1. No painel da Tailscale, restrinja a ACL: só o grupo do time alcança"
echo "     tag/servidor ${HOSTNAME_TS} na porta ${SERVE_PORT}."
echo "  2. Desative a expiração da chave deste servidor (Machines → Disable key expiry)."
echo "  3. Rode o deploy do monitoramento para aplicar GRAFANA_ROOT_URL."
