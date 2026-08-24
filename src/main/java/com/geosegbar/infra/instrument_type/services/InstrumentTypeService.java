package com.geosegbar.infra.instrument_type.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.NotFoundException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument_type.dtos.InstrumentTypeDTO;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Catálogo de tipos de instrumento, escopado por cliente.
 *
 * O tipo pertence a um cliente e só pode ser usado nas barragens desse cliente
 * (a validação de uso fica em InstrumentService, no cadastro/edição do
 * instrumento). Dentro do cliente o catálogo é compartilhado: renomear um tipo
 * reflete em todas as barragens daquele cliente — nunca nas de outro.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InstrumentTypeService {

    private final InstrumentTypeRepository instrumentTypeRepository;
    private final ClientRepository clientRepository;
    private final InstrumentRepository instrumentRepository;

    // ---------------------------------------------------------------- leitura
    /**
     * Lista os tipos visíveis ao usuário. Admin vê tudo; os demais veem apenas
     * os tipos dos clientes a que têm acesso — mais os legados, que seguem
     * visíveis para não sumirem dos instrumentos que já os usam.
     */
    @Transactional(readOnly = true)
    public List<InstrumentTypeDTO> findAll() {
        assertCanView();

        if (AuthenticatedUserUtil.isAdmin()) {
            return toDTOList(instrumentTypeRepository.findAllByOrderByNameAsc());
        }

        List<Long> clientIds = currentUserClientIds();
        List<InstrumentTypeEntity> visible = clientIds.isEmpty()
                ? new ArrayList<>()
                : new ArrayList<>(instrumentTypeRepository.findByClientIdInOrderByNameAsc(clientIds));
        visible.addAll(instrumentTypeRepository.findByClientIsNullOrderByNameAsc());

        return toDTOList(visible);
    }

    @Transactional(readOnly = true)
    public List<InstrumentTypeDTO> findByClientId(Long clientId) {
        assertCanView();
        assertClientExists(clientId);
        assertClientAccess(clientId);

        List<InstrumentTypeEntity> types
                = new ArrayList<>(instrumentTypeRepository.findByClientIdOrderByNameAsc(clientId));

        // Tipos legados entram na lista para que a tela de instrumento continue
        // conseguindo exibir o tipo dos instrumentos cadastrados antes da migração.
        types.addAll(instrumentTypeRepository.findByClientIsNullOrderByNameAsc());

        return toDTOList(types);
    }

    @Transactional(readOnly = true)
    public InstrumentTypeDTO findById(Long id) {
        assertCanView();
        InstrumentTypeEntity entity = getEntityById(id);
        if (entity.getClient() != null) {
            assertClientAccess(entity.getClient().getId());
        }
        return toDTO(entity, instrumentTypeRepository.countInstrumentsByTypeId(id),
                instrumentTypeRepository.countDamsByTypeId(id));
    }

    @Transactional(readOnly = true)
    public InstrumentTypeEntity getEntityById(Long id) {
        return instrumentTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tipo de instrumento não encontrado com ID: " + id));
    }

    // ----------------------------------------------------------------- escrita
    @Transactional
    public InstrumentTypeDTO create(InstrumentTypeDTO dto) {
        assertCanEdit();

        if (dto.getClientId() == null) {
            throw new InvalidInputException("O tipo de instrumento deve estar associado a um cliente!");
        }

        ClientEntity client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado com ID: " + dto.getClientId()));
        assertClientAccess(client.getId());

        String name = normalizeName(dto.getName());

        if (instrumentTypeRepository.existsByNameAndClientId(name, client.getId())) {
            throw new DuplicateResourceException(
                    "Já existe um tipo de instrumento com o nome '" + name + "' para o cliente " + client.getName() + ".");
        }

        InstrumentTypeEntity entity = new InstrumentTypeEntity();
        entity.setName(name);
        entity.setClient(client);

        InstrumentTypeEntity saved = instrumentTypeRepository.save(entity);
        log.info("Tipo de instrumento '{}' criado para o cliente {} (ID {})", name, client.getName(), client.getId());

        return toDTO(saved, 0L, 0L);
    }

    /**
     * Renomeia o tipo. O cliente dono nunca muda: trocá-lo levaria junto todos
     * os instrumentos já cadastrados nas barragens do cliente atual.
     */
    @Transactional
    public InstrumentTypeDTO update(Long id, InstrumentTypeDTO dto) {
        assertCanEdit();

        InstrumentTypeEntity entity = getEntityById(id);

        if (entity.getClient() == null) {
            throw new BusinessRuleException(
                    "Este tipo de instrumento é anterior à separação por cliente e ainda não foi atrelado a nenhum. "
                    + "Editá-lo agora afetaria as barragens de todos os clientes que o utilizam. "
                    + "Peça ao administrador do sistema para migrar o tipo antes de alterá-lo.");
        }

        assertClientAccess(entity.getClient().getId());

        if (dto.getClientId() != null && !dto.getClientId().equals(entity.getClient().getId())) {
            throw new BusinessRuleException(
                    "Não é permitido mudar o cliente de um tipo de instrumento. "
                    + "Crie um novo tipo no cliente de destino.");
        }

        String name = normalizeName(dto.getName());

        if (instrumentTypeRepository.existsByNameAndClientIdAndIdNot(name, entity.getClient().getId(), id)) {
            throw new DuplicateResourceException(
                    "Já existe um tipo de instrumento com o nome '" + name + "' para o cliente "
                    + entity.getClient().getName() + ".");
        }

        String previousName = entity.getName();
        entity.setName(name);
        InstrumentTypeEntity saved = instrumentTypeRepository.save(entity);

        long instrumentsCount = instrumentTypeRepository.countInstrumentsByTypeId(id);
        long damsCount = instrumentTypeRepository.countDamsByTypeId(id);

        log.info("Tipo de instrumento {} renomeado de '{}' para '{}' no cliente {} — reflete em {} instrumento(s) de {} barragem(ns) desse cliente.",
                id, previousName, name, entity.getClient().getId(), instrumentsCount, damsCount);

        return toDTO(saved, instrumentsCount, damsCount);
    }

    /**
     * Exclui o tipo. Recusa quando há instrumentos usando — apagar removeria a
     * classificação de instrumentos com histórico de leituras.
     */
    @Transactional
    public void delete(Long id) {
        assertCanEdit();

        InstrumentTypeEntity entity = getEntityById(id);

        if (entity.getClient() == null) {
            throw new BusinessRuleException(
                    "Este tipo de instrumento é anterior à separação por cliente e ainda não foi atrelado a nenhum. "
                    + "Peça ao administrador do sistema para migrá-lo antes de excluir.");
        }

        assertClientAccess(entity.getClient().getId());

        long instrumentsCount = instrumentTypeRepository.countInstrumentsByTypeId(id);
        if (instrumentsCount > 0) {
            long damsCount = instrumentTypeRepository.countDamsByTypeId(id);
            throw new BusinessRuleException(
                    "Não é possível excluir o tipo '" + entity.getName() + "' pois ele está em uso por "
                    + instrumentsCount + " instrumento(s) em " + damsCount + " barragem(ns). "
                    + "Troque o tipo desses instrumentos antes de excluir.");
        }

        instrumentTypeRepository.delete(entity);
        log.info("Tipo de instrumento {} ('{}') excluído do cliente {}.", id, entity.getName(), entity.getClient().getId());
    }

    // -------------------------------------------------- troca de cliente da barragem
    /**
     * Reaponta os instrumentos de uma barragem que mudou de cliente para os tipos
     * equivalentes do cliente novo, criando o que faltar com o mesmo nome.
     *
     * Sem isso a barragem ficaria apontando para o catálogo do cliente antigo:
     * qualquer edição posterior do instrumento passaria a ser recusada, e uma
     * alteração no tipo do cliente antigo continuaria refletindo nessa barragem —
     * exatamente o vazamento entre clientes que a separação existe para evitar.
     *
     * O catálogo do cliente antigo não é tocado: outras barragens dele seguem
     * usando os mesmos tipos.
     */
    @Transactional
    public int realignInstrumentTypesOnDamClientChange(DamEntity dam) {
        ClientEntity newClient = dam.getClient();
        if (newClient == null) {
            return 0;
        }

        List<InstrumentEntity> instruments = instrumentRepository.findByDamId(dam.getId());
        if (instruments.isEmpty()) {
            return 0;
        }

        Map<Long, InstrumentTypeEntity> equivalentByOldTypeId = new HashMap<>();
        List<InstrumentEntity> touched = new ArrayList<>();

        for (InstrumentEntity instrument : instruments) {
            InstrumentTypeEntity currentType = instrument.getInstrumentType();

            // Tipo legado (sem cliente) fica como está: ele ainda não pertence a
            // ninguém e continua válido em qualquer barragem até ser migrado.
            if (currentType == null || currentType.getClient() == null) {
                continue;
            }
            if (newClient.getId().equals(currentType.getClient().getId())) {
                continue;
            }

            InstrumentTypeEntity equivalent = equivalentByOldTypeId.computeIfAbsent(
                    currentType.getId(), oldTypeId -> findOrCreateEquivalent(currentType, newClient));

            instrument.setInstrumentType(equivalent);
            touched.add(instrument);
        }

        if (touched.isEmpty()) {
            return 0;
        }

        instrumentRepository.saveAll(touched);
        log.info("Barragem {} mudou para o cliente {}: {} instrumento(s) reapontados para os tipos do novo cliente.",
                dam.getId(), newClient.getId(), touched.size());

        return touched.size();
    }

    private InstrumentTypeEntity findOrCreateEquivalent(InstrumentTypeEntity sourceType, ClientEntity newClient) {
        return instrumentTypeRepository
                .findByClientIdAndNameIgnoreCase(newClient.getId(), sourceType.getName())
                .orElseGet(() -> {
                    InstrumentTypeEntity copy = new InstrumentTypeEntity();
                    copy.setName(sourceType.getName());
                    copy.setClient(newClient);
                    InstrumentTypeEntity saved = instrumentTypeRepository.save(copy);
                    log.info("Tipo '{}' criado no cliente {} para receber instrumentos de barragem transferida.",
                            saved.getName(), newClient.getId());
                    return saved;
                });
    }

    // ------------------------------------------------------------- permissões
    private void assertCanView() {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        InstrumentationPermissionEntity permission = AuthenticatedUserUtil.getCurrentUser().getInstrumentationPermission();
        if (permission == null || !Boolean.TRUE.equals(permission.getViewInstruments())) {
            throw new ForbiddenException("Usuário não tem permissão para visualizar tipos de instrumento!");
        }
    }

    private void assertCanEdit() {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        InstrumentationPermissionEntity permission = AuthenticatedUserUtil.getCurrentUser().getInstrumentationPermission();
        if (permission == null || !Boolean.TRUE.equals(permission.getEditInstruments())) {
            throw new ForbiddenException("Usuário não tem permissão para gerenciar tipos de instrumento!");
        }
    }

    /**
     * Impede que alguém liste ou altere o catálogo de um cliente ao qual não
     * está associado — é o que mantém os níveis de acesso por cliente.
     */
    private void assertClientAccess(Long clientId) {
        if (AuthenticatedUserUtil.isAdmin()) {
            return;
        }
        if (!currentUserClientIds().contains(clientId)) {
            throw new ForbiddenException("Usuário não tem acesso ao cliente informado!");
        }
    }

    private List<Long> currentUserClientIds() {
        UserEntity user = AuthenticatedUserUtil.getCurrentUser();
        Set<ClientEntity> clients = user.getClients();
        if (clients == null || clients.isEmpty()) {
            return List.of();
        }
        return clients.stream().map(ClientEntity::getId).collect(Collectors.toList());
    }

    private void assertClientExists(Long clientId) {
        if (!clientRepository.existsById(clientId)) {
            throw new NotFoundException("Cliente não encontrado com ID: " + clientId);
        }
    }

    // ------------------------------------------------------------- mapeamento
    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Nome do tipo de instrumento é obrigatório");
        }
        return name.trim().toUpperCase();
    }

    private List<InstrumentTypeDTO> toDTOList(List<InstrumentTypeEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }

        List<Long> ids = entities.stream().map(InstrumentTypeEntity::getId).collect(Collectors.toList());
        Map<Long, Long> usageByType = new HashMap<>();
        for (Object[] row : instrumentTypeRepository.countInstrumentsByTypeIds(ids)) {
            usageByType.put((Long) row[0], (Long) row[1]);
        }

        return entities.stream()
                // damsCount fica de fora da listagem para não disparar uma query por
                // linha; a tela de edição busca o tipo por ID e recebe o número lá.
                .map(entity -> toDTO(entity, usageByType.getOrDefault(entity.getId(), 0L), null))
                .collect(Collectors.toList());
    }

    private InstrumentTypeDTO toDTO(InstrumentTypeEntity entity, Long instrumentsCount, Long damsCount) {
        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setInstrumentsCount(instrumentsCount);
        dto.setDamsCount(damsCount);

        ClientEntity client = entity.getClient();
        if (client != null) {
            dto.setClientId(client.getId());
            dto.setClientName(client.getName());
            dto.setLegacy(false);
        } else {
            dto.setLegacy(true);
        }

        return dto;
    }
}
