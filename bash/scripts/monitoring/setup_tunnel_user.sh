#!/usr/bin/env bash
#
# Acesso de emergência ao Grafana por túnel SSH, sem shell.
#
# Cria (uma vez) o usuário `gsb-monitor`, sem senha e sem shell, e autoriza uma
# chave pública que SÓ pode abrir túnel para 127.0.0.1:3000 (o Grafana). Quem
# tem essa chave não executa comando nenhum no servidor.
#
#   sudo bash bash/scripts/monitoring/setup_tunnel_user.sh /caminho/chave.pub
#
# Do lado de quem acessa:
#   ssh -N -L 3000:127.0.0.1:3000 -p <porta-ssh> gsb-monitor@<servidor>
#   e abra http://localhost:3000

set -Eeuo pipefail

USUARIO="gsb-monitor"
CHAVE_PUB="${1:-}"

die() { echo "❌ $*" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "Rode como root."
[ -n "$CHAVE_PUB" ] && [ -f "$CHAVE_PUB" ] || die "Uso: $0 /caminho/chave.pub"
grep -Eq '^(ssh-ed25519|ecdsa-sha2-nistp256|ssh-rsa) [A-Za-z0-9+/=]+' "$CHAVE_PUB" \
  || die "Arquivo não parece uma chave pública SSH."

if ! id "$USUARIO" >/dev/null 2>&1; then
  useradd --system --create-home --shell /sbin/nologin "$USUARIO"
  passwd -l "$USUARIO" >/dev/null
  echo "✅ Usuário $USUARIO criado (sem senha, sem shell)."
fi

HOME_DIR="$(getent passwd "$USUARIO" | cut -d: -f6)"
install -d -m 700 -o "$USUARIO" -g "$USUARIO" "$HOME_DIR/.ssh"
AUTH="$HOME_DIR/.ssh/authorized_keys"
touch "$AUTH"

CHAVE="$(awk '{print $1" "$2}' "$CHAVE_PUB")"
COMENTARIO="$(awk '{print $3}' "$CHAVE_PUB")"
if grep -qF "$CHAVE" "$AUTH"; then
  echo "ℹ️  Chave já autorizada."
else
  echo "restrict,port-forwarding,permitopen=\"127.0.0.1:3000\",command=\"/bin/false\" $CHAVE ${COMENTARIO:-gsb-monitor}" >> "$AUTH"
  echo "✅ Chave autorizada só para túnel até 127.0.0.1:3000."
fi
chown "$USUARIO:$USUARIO" "$AUTH"
chmod 600 "$AUTH"

# O endurecimento do sshd pode ter desligado encaminhamento ou restringido usuários.
CONF="$(sshd -T 2>/dev/null || true)"
echo "$CONF" | grep -qi '^allowtcpforwarding \(yes\|local\|all\)' \
  || echo "⚠️  AllowTcpForwarding está desligado no sshd: libere com um Match User $USUARIO."
if echo "$CONF" | grep -qi '^allowusers '; then
  echo "$CONF" | grep -i '^allowusers ' | grep -qw "$USUARIO" \
    || echo "⚠️  O sshd tem AllowUsers e $USUARIO não está na lista."
fi
