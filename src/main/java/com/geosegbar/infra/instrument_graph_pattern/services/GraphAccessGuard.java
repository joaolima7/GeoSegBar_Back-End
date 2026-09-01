package com.geosegbar.infra.instrument_graph_pattern.services;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument_graph_customization_properties.persistence.jpa.InstrumentGraphCustomizationPropertiesRepository;
import com.geosegbar.infra.instrument_graph_pattern.persistence.jpa.InstrumentGraphPatternRepository;
import com.geosegbar.infra.instrument_graph_pattern_folder.persistence.jpa.InstrumentGraphPatternFolderRepository;
import com.geosegbar.infra.dam.services.DamAccessService;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Autorização do domínio de gráficos.
 *
 * Até então nenhuma rota de gráfico verificava nada: as flags viewGraphs,
 * editGraphsLocal e editGraphsDefault existiam no cadastro, eram gravadas pela
 * tela de permissões e nunca lidas por ninguém. Qualquer usuário autenticado
 * criava, renomeava e apagava padrão de gráfico — inclusive de barragem de
 * outro cliente, porque também não havia recorte por barragem.
 *
 * São duas checagens independentes, nesta ordem:
 *
 * 1. A barragem. Mesma regra do preenchimento de checklist: pertencer ao
 * cliente dono da barragem E ter DamPermission com acesso. Sem isso, o usuário
 * nem deveria saber que aquele gráfico existe.
 *
 * 2. A permissão de gráfico, que distingue dois tipos de padrão:
 *
 * - o "Padrão Automático - <instrumento>", criado pelo sistema junto com o
 * instrumento, responde por editGraphsDefault;
 * - qualquer outro, criado por gente, responde por editGraphsLocal.
 *
 * Essa é a leitura que o modelo permite hoje — o tipo do padrão só existe como
 * prefixo do nome, não há coluna. Em produção as duas flags estão sempre com o
 * mesmo valor (13 usuários com ambas, 11 sem nenhuma), então a distinção não
 * altera o resultado para ninguém hoje; ela existe para o dia em que alguém
 * conceder uma sem a outra. Se a semântica pretendida for a inversa, é trocar
 * os dois métodos abaixo.
 */
@Component
@RequiredArgsConstructor
public class GraphAccessGuard {

    /**
     * Prefixo do padrão que o sistema cria sozinho ao cadastrar o instrumento.
     */
    public static final String AUTO_PATTERN_PREFIX = "Padrão Automático - ";

    private final InstrumentGraphPatternRepository patternRepository;
    private final InstrumentGraphPatternFolderRepository folderRepository;
    private final InstrumentGraphCustomizationPropertiesRepository propertiesRepository;
    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;
    private final DamAccessService damAccessService;

    // ------------------------------------------------------------- leitura
    @Transactional(readOnly = true)
    public void checkViewByDam(Long damId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        UserEntity user = currentUserWithPermissions();
        requireDamAccess(user, damId);
        requireViewGraphs(user);
    }

    @Transactional(readOnly = true)
    public void checkViewByPattern(Long patternId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        checkViewByDam(damIdOfPattern(patternId));
    }

    @Transactional(readOnly = true)
    public void checkViewByInstrument(Long instrumentId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        checkViewByDam(damIdOfInstrument(instrumentId));
    }

    @Transactional(readOnly = true)
    public void checkViewByFolder(Long folderId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        checkViewByDam(damIdOfFolder(folderId));
    }

    // ------------------------------------------------------------- escrita
    /**
     * Edição de um padrão existente — e, por tabela, dos eixos e das
     * propriedades que pendem dele.
     */
    @Transactional(readOnly = true)
    public void checkEditPattern(Long patternId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }

        PatternScope scope = patternRepository.findScopeById(patternId)
                .orElseThrow(() -> new NotFoundException("Padrão de Gráfico não encontrado com ID: " + patternId));

        UserEntity user = currentUserWithPermissions();
        requireDamAccess(user, scope.getDamId());
        requireEditGraphs(user, isAutoPattern(scope.getName()));
    }

    /**
     * Criação de padrão: ainda não há padrão, então o alvo é o instrumento. Um
     * padrão criado por gente é sempre "local" — o automático só nasce junto do
     * instrumento, por dentro.
     */
    @Transactional(readOnly = true)
    public void checkCreatePatternForInstrument(Long instrumentId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        UserEntity user = currentUserWithPermissions();
        requireDamAccess(user, damIdOfInstrument(instrumentId));
        requireEditGraphs(user, false);
    }

    @Transactional(readOnly = true)
    public void checkEditProperty(Long propertyId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        Long patternId = propertiesRepository.findPatternIdById(propertyId)
                .orElseThrow(() -> new NotFoundException("Propriedade não encontrada com ID: " + propertyId));
        checkEditPattern(patternId);
    }

    /**
     * Pasta agrupa padrões criados por gente, então responde por
     * editGraphsLocal.
     */
    @Transactional(readOnly = true)
    public void checkEditFolder(Long folderId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        UserEntity user = currentUserWithPermissions();
        requireDamAccess(user, damIdOfFolder(folderId));
        requireEditGraphs(user, false);
    }

    @Transactional(readOnly = true)
    public void checkCreateFolderInDam(Long damId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        UserEntity user = currentUserWithPermissions();
        requireDamAccess(user, damId);
        requireEditGraphs(user, false);
    }

    // ------------------------------------------------------------- interno
    public static boolean isAutoPattern(String patternName) {
        return patternName != null && patternName.startsWith(AUTO_PATTERN_PREFIX);
    }

    /**
     * O usuário do token vem do filtro de autenticação e pode estar destacado,
     * com as coleções de permissão não inicializadas. Recarrega com o mesmo
     * EntityGraph que o preenchimento de checklist usa.
     */
    private UserEntity currentUserWithPermissions() {
        UserEntity fromToken = AuthenticatedUserUtil.getCurrentUser();
        return userRepository.findByIdWithPermissions(fromToken.getId())
                .orElseThrow(() -> new NotFoundException("Usuário logado não encontrado"));
    }

    /**
     * Delega ao DamAccessService, que é a fonte única da regra de acesso a
     * barragem. A cópia que existia aqui conferia só a DamPermission e não a
     * associação com o cliente — era mais frouxa que a regra do preenchimento
     * de checklist, para a mesma pergunta.
     *
     * O parâmetro user permanece porque quem chama já carregou o usuário para
     * ler as flags de gráfico, e trocar a assinatura não melhoraria nada.
     */
    private void requireDamAccess(UserEntity user, Long damId) {
        damAccessService.requireAccess(damId);
    }

    private void requireViewGraphs(UserEntity user) {
        InstrumentationPermissionEntity permission = user.getInstrumentationPermission();
        if (permission == null || !Boolean.TRUE.equals(permission.getViewGraphs())) {
            throw new ForbiddenException("Usuário não tem permissão para visualizar gráficos.");
        }
    }

    private void requireEditGraphs(UserEntity user, boolean autoPattern) {
        InstrumentationPermissionEntity permission = user.getInstrumentationPermission();

        boolean allowed = permission != null && Boolean.TRUE.equals(
                autoPattern ? permission.getEditGraphsDefault() : permission.getEditGraphsLocal());

        if (!allowed) {
            throw new ForbiddenException(autoPattern
                    ? "Usuário não tem permissão para editar gráficos padrão."
                    : "Usuário não tem permissão para editar gráficos locais.");
        }
    }

    private Long damIdOfPattern(Long patternId) {
        return patternRepository.findScopeById(patternId)
                .orElseThrow(() -> new NotFoundException("Padrão de Gráfico não encontrado com ID: " + patternId))
                .getDamId();
    }

    private Long damIdOfInstrument(Long instrumentId) {
        return instrumentRepository.findDamIdById(instrumentId)
                .orElseThrow(() -> new NotFoundException("Instrumento não encontrado com ID: " + instrumentId));
    }

    private Long damIdOfFolder(Long folderId) {
        return folderRepository.findDamIdById(folderId)
                .orElseThrow(() -> new NotFoundException("Pasta não encontrada com ID: " + folderId));
    }

    /**
     * Projeção mínima: só o que a autorização precisa saber sobre um padrão.
     */
    public interface PatternScope {

        Long getDamId();

        String getName();
    }
}
