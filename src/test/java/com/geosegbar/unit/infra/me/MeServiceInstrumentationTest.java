package com.geosegbar.unit.infra.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.common.utils.AuthenticatedUserUtil;
import com.geosegbar.entities.InstrumentationPermissionEntity;
import com.geosegbar.entities.RoleEntity;
import com.geosegbar.entities.UserEntity;
import com.geosegbar.infra.me.dtos.MePermissionsDTO;
import com.geosegbar.infra.me.services.MeService;
import com.geosegbar.infra.user.persistence.jpa.UserRepository;

/**
 * /me/permissions ganha a instrumentação (V5-B04) para o app saber se pode
 * oferecer a correção de uma leitura já registrada.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("MeService — permissões de instrumentação")
class MeServiceInstrumentationTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MeService meService;

    private MockedStatic<AuthenticatedUserUtil> auth;
    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setId(5L);
        auth = Mockito.mockStatic(AuthenticatedUserUtil.class);
        auth.when(AuthenticatedUserUtil::getCurrentUser).thenReturn(user);
        when(userRepository.findByIdWithPermissions(5L)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        auth.close();
    }

    private void role(RoleEnum name) {
        RoleEntity role = new RoleEntity();
        role.setName(name);
        user.setRole(role);
    }

    @Test
    @DisplayName("colaborador leva o que a permissão de instrumentação diz")
    void collaboratorCarriesInstrumentationPermission() {
        role(RoleEnum.COLLABORATOR);
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setViewInstruments(true);
        permission.setViewRead(true);
        permission.setEditRead(true);
        user.setInstrumentationPermission(permission);

        MePermissionsDTO dto = meService.currentUserPermissions();

        assertThat(dto.instrumentation()).isEqualTo(new MePermissionsDTO.Instrumentation(true, true, true));
    }

    @Test
    @DisplayName("colaborador sem permissão de instrumentação não pode nada")
    void collaboratorWithoutPermissionGetsNone() {
        role(RoleEnum.COLLABORATOR);

        assertThat(meService.currentUserPermissions().instrumentation())
                .isEqualTo(MePermissionsDTO.Instrumentation.NONE);
    }

    @Test
    @DisplayName("ADMIN pode tudo, como na validação das leituras")
    void adminCanEverything() {
        role(RoleEnum.ADMIN);

        assertThat(meService.currentUserPermissions().instrumentation())
                .isEqualTo(MePermissionsDTO.Instrumentation.ALL);
    }
}
