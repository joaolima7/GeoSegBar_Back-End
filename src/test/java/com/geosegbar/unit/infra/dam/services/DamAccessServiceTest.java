package com.geosegbar.unit.infra.dam.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.exceptions.ForbiddenException;
import com.geosegbar.infra.dam.persistence.jpa.DamRepository;
import com.geosegbar.infra.dam.services.DamAccessService;

/**
 * O recorte de barragem por usuário, que antes existia escrito em três lugares
 * diferentes e agora mora só aqui.
 *
 * O que motivou: o app recebia no aparelho os checklists e instrumentos das 12
 * barragens do cliente mesmo quando o inspetor só tinha acesso a 2 — e as
 * rotas de gráfico não conferiam barragem nenhuma, nem entre clientes.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests - DamAccessService")
class DamAccessServiceTest {

    @Mock
    private DamRepository damRepository;

    @InjectMocks
    private DamAccessService damAccessService;

    private MockedStatic<AuthenticatedUserUtil> auth;
    private UserEntity usuario;

    @BeforeEach
    void setUp() {
        usuario = new UserEntity();
        usuario.setId(7L);

        auth = Mockito.mockStatic(AuthenticatedUserUtil.class);
        auth.when(AuthenticatedUserUtil::getCurrentUser).thenReturn(usuario);
        auth.when(AuthenticatedUserUtil::isAdmin).thenReturn(false);
    }

    @AfterEach
    void tearDown() {
        auth.close();
    }

    private void comAcessoA(Long... ids) {
        lenient().when(damRepository.findAccessibleDamIdsByUserId(7L)).thenReturn(List.of(ids));
    }

    @Test
    @DisplayName("Devolve apenas as barragens permitidas")
    void devolveApenasPermitidas() {
        comAcessoA(3L, 7L);
        assertThat(damAccessService.accessibleDamIds()).containsExactly(3L, 7L);
    }

    @Test
    @DisplayName("ADMIN vê todas sem consultar permissão")
    void adminVeTodas() {
        auth.when(AuthenticatedUserUtil::isAdmin).thenReturn(true);
        when(damRepository.findAllDamIds()).thenReturn(List.of(1L, 2L, 3L));

        assertThat(damAccessService.accessibleDamIds()).containsExactly(1L, 2L, 3L);
        verify(damRepository, never()).findAccessibleDamIdsByUserId(anyLong());
    }

    @Test
    @DisplayName("Barragem fora da permissão é barrada com 403")
    void barragemDeOutroClienteEhBarrada() {
        comAcessoA(3L);

        assertThatThrownBy(() -> damAccessService.requireAccess(99L))
                .isInstanceOf(ForbiddenException.class);

        assertThatCode(() -> damAccessService.requireAccess(3L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("damId nulo nunca é acesso permitido")
    void damIdNuloNaoPassa() {
        comAcessoA(3L);
        assertThat(damAccessService.hasAccess(null)).isFalse();
    }

    @Test
    @DisplayName("Pedido sem escopo devolve tudo que o usuário pode ver")
    void semEscopoDevolveTodasPermitidas() {
        comAcessoA(3L, 7L);

        assertThat(damAccessService.intersectWithAccessible(null)).containsExactly(3L, 7L);
        assertThat(damAccessService.intersectWithAccessible(List.of())).containsExactly(3L, 7L);
    }

    @Test
    @DisplayName("Id defasado no cache do app é ignorado, não derruba a chamada")
    void idDefasadoEhIgnorado() {
        // O app trabalha com permissão cacheada; a defasagem em relação ao
        // servidor é normal. Lançar 403 porque um id envelheceu zeraria a tela
        // inteira do inspetor por um comportamento previsto do sistema.
        comAcessoA(3L, 7L);

        assertThat(damAccessService.intersectWithAccessible(List.of(3L, 11L))).containsExactly(3L);
        assertThat(damAccessService.ignoredFrom(List.of(3L, 11L))).containsExactly(11L);
    }

    @Test
    @DisplayName("Nenhuma barragem permitida devolve vazio, não tudo")
    void semPermissaoDevolveVazio() {
        comAcessoA();

        assertThat(damAccessService.accessibleDamIds()).isEmpty();
        assertThat(damAccessService.intersectWithAccessible(List.of(3L, 7L))).isEmpty();
    }
}
