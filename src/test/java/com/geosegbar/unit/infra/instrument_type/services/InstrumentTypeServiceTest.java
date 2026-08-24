package com.geosegbar.unit.infra.instrument_type.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.InstrumentEntity;
import com.geosegbar.entities.InstrumentTypeEntity;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.BusinessRuleException;
import com.geosegbar.exceptions.DuplicateResourceException;
import com.geosegbar.exceptions.InvalidInputException;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.client.persistence.jpa.ClientRepository;
import com.geosegbar.infra.instrument.persistence.jpa.InstrumentRepository;
import com.geosegbar.infra.instrument_type.dtos.InstrumentTypeDTO;
import com.geosegbar.infra.instrument_type.persistence.jpa.InstrumentTypeRepository;
import com.geosegbar.infra.instrument_type.services.InstrumentTypeService;

@Tag("unit")
@DisplayName("Unit Tests - InstrumentTypeService (catálogo por cliente)")
class InstrumentTypeServiceTest extends BaseUnitTest {

    private static final Long CLIENT_A = 1L;
    private static final Long CLIENT_B = 2L;

    @Mock
    private InstrumentTypeRepository instrumentTypeRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private InstrumentTypeService instrumentTypeService;

    private MockedStatic<AuthenticatedUserUtil> authUtil;
    private ClientEntity clientA;

    @BeforeEach
    void setUp() {
        clientA = client(CLIENT_A, "Cliente A");
        authUtil = mockStatic(AuthenticatedUserUtil.class);
        authUtil.when(AuthenticatedUserUtil::isAdmin).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        authUtil.close();
    }

    // ------------------------------------------------------------------ create
    @Test
    @DisplayName("Recusa criação sem cliente — o tipo precisa de dono")
    void shouldRejectCreateWithoutClient() {
        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setName("PIEZÔMETRO");

        assertThatThrownBy(() -> instrumentTypeService.create(dto))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("associado a um cliente");

        verify(instrumentTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Recusa nome repetido dentro do mesmo cliente")
    void shouldRejectDuplicateNameWithinSameClient() {
        when(clientRepository.findById(CLIENT_A)).thenReturn(Optional.of(clientA));
        when(instrumentTypeRepository.existsByNameAndClientId("PIEZÔMETRO", CLIENT_A)).thenReturn(true);

        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setName("Piezômetro");
        dto.setClientId(CLIENT_A);

        assertThatThrownBy(() -> instrumentTypeService.create(dto))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Cliente A");

        verify(instrumentTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Permite o mesmo nome em clientes diferentes — catálogos são independentes")
    void shouldAllowSameNameForDifferentClient() {
        ClientEntity clientB = client(CLIENT_B, "Cliente B");
        when(clientRepository.findById(CLIENT_B)).thenReturn(Optional.of(clientB));
        // Cliente B ainda não tem o tipo, mesmo que o Cliente A tenha.
        when(instrumentTypeRepository.existsByNameAndClientId("PIEZÔMETRO", CLIENT_B)).thenReturn(false);
        when(instrumentTypeRepository.save(any(InstrumentTypeEntity.class)))
                .thenAnswer(invocation -> {
                    InstrumentTypeEntity saved = invocation.getArgument(0);
                    saved.setId(99L);
                    return saved;
                });

        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setName("piezômetro");
        dto.setClientId(CLIENT_B);

        InstrumentTypeDTO created = instrumentTypeService.create(dto);

        assertThat(created.getId()).isEqualTo(99L);
        assertThat(created.getName()).isEqualTo("PIEZÔMETRO");
        assertThat(created.getClientId()).isEqualTo(CLIENT_B);
        assertThat(created.getLegacy()).isFalse();
    }

    // ------------------------------------------------------------------ update
    @Test
    @DisplayName("Bloqueia edição de tipo legado — alteração vazaria entre clientes")
    void shouldBlockUpdateOfLegacyType() {
        InstrumentTypeEntity legacy = type(5L, "PIEZÔMETRO", null);
        when(instrumentTypeRepository.findById(5L)).thenReturn(Optional.of(legacy));

        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setName("MEDIDOR DE NÍVEL");

        assertThatThrownBy(() -> instrumentTypeService.update(5L, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("anterior à separação por cliente");

        verify(instrumentTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bloqueia troca de cliente do tipo")
    void shouldBlockChangingOwningClient() {
        InstrumentTypeEntity entity = type(5L, "PIEZÔMETRO", clientA);
        when(instrumentTypeRepository.findById(5L)).thenReturn(Optional.of(entity));

        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setName("PIEZÔMETRO");
        dto.setClientId(CLIENT_B);

        assertThatThrownBy(() -> instrumentTypeService.update(5L, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mudar o cliente");

        verify(instrumentTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Renomeia e devolve o impacto dentro do cliente")
    void shouldRenameAndReportImpact() {
        InstrumentTypeEntity entity = type(5L, "PIEZÔMETRO", clientA);
        when(instrumentTypeRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(instrumentTypeRepository.existsByNameAndClientIdAndIdNot("PIEZÔMETRO ELÉTRICO", CLIENT_A, 5L))
                .thenReturn(false);
        when(instrumentTypeRepository.save(any(InstrumentTypeEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(instrumentTypeRepository.countInstrumentsByTypeId(5L)).thenReturn(12L);
        when(instrumentTypeRepository.countDamsByTypeId(5L)).thenReturn(3L);

        InstrumentTypeDTO dto = new InstrumentTypeDTO();
        dto.setName("Piezômetro Elétrico");

        InstrumentTypeDTO updated = instrumentTypeService.update(5L, dto);

        assertThat(updated.getName()).isEqualTo("PIEZÔMETRO ELÉTRICO");
        assertThat(updated.getClientId()).isEqualTo(CLIENT_A);
        assertThat(updated.getInstrumentsCount()).isEqualTo(12L);
        assertThat(updated.getDamsCount()).isEqualTo(3L);
    }

    // ------------------------------------------------------------------ delete
    @Test
    @DisplayName("Recusa exclusão de tipo em uso")
    void shouldRejectDeleteWhenInUse() {
        InstrumentTypeEntity entity = type(5L, "PIEZÔMETRO", clientA);
        when(instrumentTypeRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(instrumentTypeRepository.countInstrumentsByTypeId(5L)).thenReturn(4L);
        when(instrumentTypeRepository.countDamsByTypeId(5L)).thenReturn(2L);

        assertThatThrownBy(() -> instrumentTypeService.delete(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("4 instrumento(s) em 2 barragem(ns)");

        verify(instrumentTypeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Exclui tipo sem instrumentos")
    void shouldDeleteUnusedType() {
        InstrumentTypeEntity entity = type(5L, "PIEZÔMETRO", clientA);
        when(instrumentTypeRepository.findById(5L)).thenReturn(Optional.of(entity));
        when(instrumentTypeRepository.countInstrumentsByTypeId(5L)).thenReturn(0L);

        assertThatCode(() -> instrumentTypeService.delete(5L)).doesNotThrowAnyException();

        verify(instrumentTypeRepository).delete(entity);
    }

    @Test
    @DisplayName("Bloqueia exclusão de tipo legado")
    void shouldBlockDeleteOfLegacyType() {
        InstrumentTypeEntity legacy = type(5L, "PIEZÔMETRO", null);
        when(instrumentTypeRepository.findById(5L)).thenReturn(Optional.of(legacy));

        assertThatThrownBy(() -> instrumentTypeService.delete(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("anterior à separação por cliente");

        verify(instrumentTypeRepository, never()).delete(any());
    }

    // ------------------------------------------------------- acesso por cliente
    @Test
    @DisplayName("Não-admin não enxerga o catálogo de cliente que não é dele")
    void shouldBlockAccessToForeignClientCatalog() {
        authUtil.when(AuthenticatedUserUtil::isAdmin).thenReturn(false);
        authUtil.when(AuthenticatedUserUtil::getCurrentUser).thenReturn(userWithClientAndEditPermission(CLIENT_A));
        when(clientRepository.existsById(CLIENT_B)).thenReturn(true);

        assertThatThrownBy(() -> instrumentTypeService.findByClientId(CLIENT_B))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("não tem acesso ao cliente");
    }

    @Test
    @DisplayName("Não-admin acessa o catálogo do próprio cliente")
    void shouldAllowAccessToOwnClientCatalog() {
        authUtil.when(AuthenticatedUserUtil::isAdmin).thenReturn(false);
        authUtil.when(AuthenticatedUserUtil::getCurrentUser).thenReturn(userWithClientAndEditPermission(CLIENT_A));
        when(clientRepository.existsById(CLIENT_A)).thenReturn(true);
        when(instrumentTypeRepository.findByClientIdOrderByNameAsc(CLIENT_A))
                .thenReturn(List.of(type(5L, "PIEZÔMETRO", clientA)));
        when(instrumentTypeRepository.findByClientIsNullOrderByNameAsc()).thenReturn(List.of());
        when(instrumentTypeRepository.countInstrumentsByTypeIds(List.of(5L))).thenReturn(List.of());

        List<InstrumentTypeDTO> types = instrumentTypeService.findByClientId(CLIENT_A);

        assertThat(types).hasSize(1);
        assertThat(types.get(0).getName()).isEqualTo("PIEZÔMETRO");
        assertThat(types.get(0).getInstrumentsCount()).isZero();
    }

    @Test
    @DisplayName("Sem permissão de instrumentação, não lista tipos")
    void shouldBlockViewWithoutInstrumentationPermission() {
        UserEntity user = new UserEntity();
        user.setInstrumentationPermission(null);

        authUtil.when(AuthenticatedUserUtil::isAdmin).thenReturn(false);
        authUtil.when(AuthenticatedUserUtil::getCurrentUser).thenReturn(user);

        assertThatThrownBy(() -> instrumentTypeService.findAll())
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("visualizar tipos de instrumento");
    }

    // --------------------------------------- barragem que muda de cliente
    @Test
    @DisplayName("Barragem que muda de cliente reaponta os instrumentos para o tipo equivalente do novo cliente")
    void shouldRealignInstrumentTypesWhenDamChangesClient() {
        ClientEntity clientB = client(CLIENT_B, "Cliente B");
        InstrumentTypeEntity typeOfA = type(5L, "PIEZÔMETRO", clientA);
        InstrumentTypeEntity typeOfB = type(50L, "PIEZÔMETRO", clientB);

        DamEntity dam = new DamEntity();
        dam.setId(30L);
        dam.setClient(clientB);

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(300L);
        instrument.setInstrumentType(typeOfA);

        when(instrumentRepository.findByDamId(30L)).thenReturn(List.of(instrument));
        when(instrumentTypeRepository.findByClientIdAndNameIgnoreCase(CLIENT_B, "PIEZÔMETRO"))
                .thenReturn(Optional.of(typeOfB));

        int moved = instrumentTypeService.realignInstrumentTypesOnDamClientChange(dam);

        assertThat(moved).isEqualTo(1);
        assertThat(instrument.getInstrumentType()).isSameAs(typeOfB);
        // Não cria tipo novo quando o cliente de destino já tem um de mesmo nome.
        verify(instrumentTypeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Cria o tipo no cliente de destino quando ele ainda não existe lá")
    void shouldCreateEquivalentTypeWhenMissingInTargetClient() {
        ClientEntity clientB = client(CLIENT_B, "Cliente B");
        InstrumentTypeEntity typeOfA = type(5L, "PIEZÔMETRO", clientA);

        DamEntity dam = new DamEntity();
        dam.setId(30L);
        dam.setClient(clientB);

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(300L);
        instrument.setInstrumentType(typeOfA);

        when(instrumentRepository.findByDamId(30L)).thenReturn(List.of(instrument));
        when(instrumentTypeRepository.findByClientIdAndNameIgnoreCase(CLIENT_B, "PIEZÔMETRO"))
                .thenReturn(Optional.empty());
        when(instrumentTypeRepository.save(any(InstrumentTypeEntity.class)))
                .thenAnswer(invocation -> {
                    InstrumentTypeEntity saved = invocation.getArgument(0);
                    saved.setId(77L);
                    return saved;
                });

        int moved = instrumentTypeService.realignInstrumentTypesOnDamClientChange(dam);

        assertThat(moved).isEqualTo(1);
        assertThat(instrument.getInstrumentType().getId()).isEqualTo(77L);
        assertThat(instrument.getInstrumentType().getClient()).isSameAs(clientB);
        // O tipo do cliente de origem continua intacto — outras barragens dele seguem usando.
        assertThat(typeOfA.getClient()).isSameAs(clientA);
        assertThat(typeOfA.getName()).isEqualTo("PIEZÔMETRO");
    }

    @Test
    @DisplayName("Tipo legado não é movido quando a barragem muda de cliente")
    void shouldLeaveLegacyTypeUntouchedOnClientChange() {
        ClientEntity clientB = client(CLIENT_B, "Cliente B");
        InstrumentTypeEntity legacy = type(5L, "PIEZÔMETRO", null);

        DamEntity dam = new DamEntity();
        dam.setId(30L);
        dam.setClient(clientB);

        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setId(300L);
        instrument.setInstrumentType(legacy);

        when(instrumentRepository.findByDamId(30L)).thenReturn(List.of(instrument));

        int moved = instrumentTypeService.realignInstrumentTypesOnDamClientChange(dam);

        assertThat(moved).isZero();
        assertThat(instrument.getInstrumentType()).isSameAs(legacy);
        verify(instrumentRepository, never()).saveAll(any());
    }

    // ----------------------------------------------------------------- fixtures
    private ClientEntity client(Long id, String name) {
        ClientEntity client = new ClientEntity();
        client.setId(id);
        client.setName(name);
        return client;
    }

    private InstrumentTypeEntity type(Long id, String name, ClientEntity client) {
        InstrumentTypeEntity type = new InstrumentTypeEntity();
        type.setId(id);
        type.setName(name);
        type.setClient(client);
        return type;
    }

    private UserEntity userWithClientAndEditPermission(Long clientId) {
        UserEntity user = new UserEntity();
        user.setClients(Set.of(client(clientId, "Cliente " + clientId)));

        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewInstruments(true);
        permission.setEditInstruments(true);
        user.setInstrumentationPermission(permission);

        return user;
    }
}
