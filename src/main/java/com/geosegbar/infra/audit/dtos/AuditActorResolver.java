package com.geosegbar.infra.audit.dtos;

/**
 * Resolve a exibição do ator a partir do JOIN com o usuário (dados atuais) ou do
 * rótulo de fallback ({@code actorLabel}) para atores sem conta — job, anônimo
 * ou e-mail tentado em login que falhou.
 */
final class AuditActorResolver {

    private static final String ACTOR_UNKNOWN = "Usuário removido";

    private AuditActorResolver() {
    }

    /**
     * Nome de exibição: prioriza o nome atual do usuário (JOIN); senão usa o
     * rótulo de fallback; se nenhum existir mas houver vínculo de usuário que não
     * foi encontrado, indica usuário removido.
     */
    static String resolveName(String userName, String actorLabel) {
        if (userName != null && !userName.isBlank()) {
            return userName;
        }
        if (actorLabel != null && !actorLabel.isBlank()) {
            return actorLabel;
        }
        return null;
    }

    /**
     * Igual a {@link #resolveName}, mas quando há um {@code actorUserId} sem
     * usuário correspondente (usuário deletado) e sem rótulo, devolve um indicador
     * explícito em vez de null.
     */
    static String resolveName(String userName, String actorLabel, Long actorUserId) {
        String resolved = resolveName(userName, actorLabel);
        if (resolved != null) {
            return resolved;
        }
        return actorUserId != null ? ACTOR_UNKNOWN : null;
    }
}
