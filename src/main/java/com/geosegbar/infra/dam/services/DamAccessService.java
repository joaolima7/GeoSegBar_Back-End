package com.geosegbar.infra.dam.services;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;

import lombok.RequiredArgsConstructor;

/**
 * Fonte única de "quais barragens este usuário pode ver".
 *
 * A regra já existia escrita três vezes, cada uma de um jeito: no
 * preenchimento de checklist (validação em Java sobre as coleções do usuário),
 * no quick-access de barragem (SQL) e no guard de gráficos. Três cópias da
 * mesma regra é a garantia de que um dia elas vão divergir — e uma regra de
 * permissão que diverge é um vazamento esperando a vez.
 *
 * A regra, em uma frase: o usuário precisa estar associado ao cliente dono da
 * barragem E ter DamPermission com has_access = true. ADMIN pula tudo.
 *
 * A consulta é a mesma do quick-access, que já estava correta — este serviço
 * não inventa regra nova, só passa a ser o único lugar onde ela mora.
 */
@Service
@RequiredArgsConstructor
public class DamAccessService {

    private final DamRepository damRepository;

    /**
     * Os ids das barragens que o usuário do token pode acessar.
     *
     * Para ADMIN devolve todas — mesmo atalho que o quick-access já usava.
     */
    @Transactional(readOnly = true)
    public Set<Long> accessibleDamIds() {
        List<Long> ids = AuthenticatedUserUtil.isAdmin()
                ? damRepository.findAllDamIds()
                : damRepository.findAccessibleDamIdsByUserId(
                        AuthenticatedUserUtil.getCurrentUser().getId());

        return new LinkedHashSet<>(ids);
    }

    @Transactional(readOnly = true)
    public boolean hasAccess(Long damId) {
        if (damId == null) {
            return false;
        }
        return AuthenticatedUserUtil.isAdmin() || accessibleDamIds().contains(damId);
    }

    /**
     * Barra o acesso com 403. Use quando o usuário pediu uma barragem
     * específica e não pode vê-la.
     */
    @Transactional(readOnly = true)
    public void requireAccess(Long damId) {
        if (!hasAccess(damId)) {
            throw new ForbiddenException(
                    "Usuário não tem permissão de acesso a esta barragem.");
        }
    }

    /**
     * Intersecção entre o que foi pedido e o que é permitido, sem 403.
     *
     * É o que as rotas do app precisam: ele trabalha com permissão cacheada e
     * a defasagem em relação ao servidor é normal, não excepcional. Derrubar a
     * tela inteira porque um id do cache ficou velho é punir o usuário por um
     * comportamento previsto do sistema. Quem pediu uma barragem específica e
     * precisa saber que não pode vê-la usa requireAccess.
     */
    @Transactional(readOnly = true)
    public Set<Long> intersectWithAccessible(Collection<Long> requestedDamIds) {
        Set<Long> accessible = accessibleDamIds();

        if (requestedDamIds == null || requestedDamIds.isEmpty()) {
            return accessible;
        }

        Set<Long> result = new LinkedHashSet<>(requestedDamIds);
        result.retainAll(accessible);
        return result;
    }

    /**
     * O que foi pedido e teve que ser descartado. O app mostra isso ao usuário
     * em vez de simplesmente sumir com a barragem da tela.
     */
    @Transactional(readOnly = true)
    public Set<Long> ignoredFrom(Collection<Long> requestedDamIds) {
        if (requestedDamIds == null || requestedDamIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> ignored = new LinkedHashSet<>(requestedDamIds);
        ignored.removeAll(accessibleDamIds());
        return ignored;
    }
}
