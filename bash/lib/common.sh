#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
BASH_DIR="$ROOT_DIR/bash"
LOG_DIR="$ROOT_DIR/logs/ops"
LOCK_FILE="/tmp/geosegbar-cli.lock"
LOCK_DIR="/tmp/geosegbar-cli.lockdir"

DRY_RUN="${DRY_RUN:-false}"
VERBOSE="${VERBOSE:-false}"
ASSUME_YES="${ASSUME_YES:-false}"

mkdir -p "$LOG_DIR"
RUN_TS="$(date +"%Y%m%d-%H%M%S")"
RUN_LOG_FILE="$LOG_DIR/cli-$RUN_TS.log"

log() {
    local level="$1"
    shift
    local msg="$*"
    local now
    now="$(date +"%Y-%m-%d %H:%M:%S")"
    echo "[$now] [$level] $msg" | tee -a "$RUN_LOG_FILE"
}

info() { log "INFO" "$*"; }
warn() { log "WARN" "$*"; }
error() { log "ERROR" "$*"; }

die() {
    error "$*"
    exit 1
}

run_cmd() {
    local cmd="$*"
    if [[ "$DRY_RUN" == "true" ]]; then
        info "[dry-run] $cmd"
        return 0
    fi

    info "Executando: $cmd"
    if [[ "$VERBOSE" == "true" ]]; then
        eval "$cmd"
    else
        eval "$cmd" >>"$RUN_LOG_FILE" 2>&1
    fi
}

ensure_root_dir() {
    cd "$ROOT_DIR" || die "Não foi possível acessar a raiz do projeto: $ROOT_DIR"
}

ensure_file_exists() {
    local file="$1"
    [[ -f "$file" ]] || die "Arquivo obrigatório não encontrado: $file"
}

ensure_executable() {
    local file="$1"
    [[ -x "$file" ]] || die "Script sem permissão de execução: $file"
}

ensure_docker_up() {
    if [[ "$DRY_RUN" == "true" ]]; then
        warn "Docker check ignorado em dry-run"
        return 0
    fi
    docker info >/dev/null 2>&1 || die "Docker não está rodando"
}

ensure_cli_context() {
    if [[ "${GEOSEGBAR_CLI_CONTEXT:-0}" != "1" ]]; then
        die "Execução direta não permitida. Use: ./bash/cli_app.sh"
    fi
}

authenticate_cli_action() {
    local max_attempts=3
    local attempt=1
    local typed_password

    if [[ -z "${CLI_PASSWORD:-}" ]]; then
        die "CLI_PASSWORD não definido no .env.prod"
    fi

    while [[ "$attempt" -le "$max_attempts" ]]; do
        read -r -s -p "Digite a senha da CLI: " typed_password
        echo

        if [[ "$typed_password" == "$CLI_PASSWORD" ]]; then
            info "Autenticação da ação concluída"
            return 0
        fi

        warn "Senha inválida ($attempt/$max_attempts)"
        attempt=$((attempt + 1))
    done

    die "Falha de autenticação após $max_attempts tentativas"
}

confirm() {
    local message="$1"

    if [[ "$ASSUME_YES" == "true" ]]; then
        return 0
    fi

    read -r -p "$message [y/N]: " answer
    [[ "$answer" =~ ^[Yy]$ ]]
}

load_prod_env() {
    ensure_root_dir
    ensure_file_exists "$ROOT_DIR/.env.prod"

    set -a
    # shellcheck disable=SC1091
    source "$ROOT_DIR/.env.prod"
    set +a

    info "Variáveis carregadas de .env.prod"
}

acquire_lock() {
    if command -v flock >/dev/null 2>&1; then
        exec 9>"$LOCK_FILE"
        if ! flock -n 9; then
            die "Outra execução da CLI já está em andamento (lock: $LOCK_FILE)."
        fi
        return 0
    fi

    if mkdir "$LOCK_DIR" 2>/dev/null; then
        trap 'rm -rf "$LOCK_DIR"' EXIT
        return 0
    fi

    die "Outra execução da CLI já está em andamento (lock dir: $LOCK_DIR)."
}

on_error() {
    local exit_code="$1"
    local line="$2"
    error "Falha na execução (linha: $line, código: $exit_code). Veja logs em: $RUN_LOG_FILE"
}

trap 'on_error $? $LINENO' ERR
