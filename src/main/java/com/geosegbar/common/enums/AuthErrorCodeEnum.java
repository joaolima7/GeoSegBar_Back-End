package com.geosegbar.common.enums;

/**
 * Código estável que acompanha as respostas 401 e 403, para o front decidir o
 * que fazer sem precisar interpretar a mensagem em português.
 *
 * A regra que o front precisa seguir é simples e vem do status HTTP:
 *
 *   401 -> a sessão acabou (ou nunca existiu). Deslogar e mandar para o login.
 *   403 -> está logado, mas não pode fazer isso. Mostrar o aviso e ficar onde está.
 *
 * O código abaixo serve só para escolher a mensagem certa dentro de cada caso.
 */
public enum AuthErrorCodeEnum {

    /**
     * 401 — requisição sem token. Sessão nunca existiu ou já foi limpa.
     */
    NOT_AUTHENTICATED,
    /**
     * 401 — token válido, porém vencido. É o caso clássico de "sua sessão
     * expirou", e o único em que vale mostrar essa mensagem ao usuário.
     */
    SESSION_EXPIRED,
    /**
     * 401 — token malformado, assinatura inválida ou emissor errado. Não é
     * expiração: normalmente é token corrompido no storage do navegador.
     */
    INVALID_TOKEN,
    /**
     * 401 — token íntegro, mas a conta não existe mais ou foi desativada.
     */
    ACCOUNT_UNAVAILABLE,
    /**
     * 403 — autenticado, mas sem permissão para a operação ou sem acesso ao
     * recurso. NUNCA deve provocar logout.
     */
    FORBIDDEN
}
