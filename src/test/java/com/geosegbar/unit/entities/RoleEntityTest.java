package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.RoleEntity;

@Tag("unit")
class RoleEntityTest extends BaseUnitTest {

    @Test
    @DisplayName("Should create role with all required fields")
    void shouldCreateRoleWithAllRequiredFields() {

        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setName(RoleEnum.ADMIN);
        role.setDescription("Administrador do sistema");

        assertThat(role).satisfies(r -> {
            assertThat(r.getId()).isEqualTo(1L);
            assertThat(r.getName()).isEqualTo(RoleEnum.ADMIN);
            assertThat(r.getDescription()).isEqualTo("Administrador do sistema");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        RoleEntity role = new RoleEntity(
                1L,
                RoleEnum.ADMIN,
                "Administrador com acesso total"
        );

        assertThat(role.getId()).isEqualTo(1L);
        assertThat(role.getName()).isEqualTo(RoleEnum.ADMIN);
        assertThat(role.getDescription()).isEqualTo("Administrador com acesso total");
    }

    @Test
    @DisplayName("Should create using name and description constructor")
    void shouldCreateUsingNameAndDescriptionConstructor() {

        RoleEntity role = new RoleEntity(
                RoleEnum.COLLABORATOR,
                "Colaborador do sistema"
        );

        assertThat(role.getName()).isEqualTo(RoleEnum.COLLABORATOR);
        assertThat(role.getDescription()).isEqualTo("Colaborador do sistema");
    }

    @Test
    @DisplayName("Should support RoleEnum ADMIN")
    void shouldSupportRoleEnumAdmin() {

        RoleEntity role = new RoleEntity();
        role.setName(RoleEnum.ADMIN);

        assertThat(role.getName()).isEqualTo(RoleEnum.ADMIN);
    }

    @Test
    @DisplayName("Should support RoleEnum COLLABORATOR")
    void shouldSupportRoleEnumCollaborator() {

        RoleEntity role = new RoleEntity();
        role.setName(RoleEnum.COLLABORATOR);

        assertThat(role.getName()).isEqualTo(RoleEnum.COLLABORATOR);
    }

    @Test
    @DisplayName("Should support descriptive role descriptions")
    void shouldSupportDescriptiveRoleDescriptions() {

        RoleEntity role = new RoleEntity();
        role.setDescription("Administrador com acesso total ao sistema, podendo gerenciar usuários e configurações");

        assertThat(role.getDescription()).hasSize(85);
    }

    @Test
    @DisplayName("Should support short role descriptions")
    void shouldSupportShortRoleDescriptions() {

        RoleEntity role = new RoleEntity();
        role.setDescription("Admin");

        assertThat(role.getDescription()).hasSize(5);
    }

    @Test
    @DisplayName("Should support Portuguese characters in description")
    void shouldSupportPortugueseCharactersInDescription() {

        RoleEntity role = new RoleEntity();
        role.setDescription("Usuário comum com permissões básicas de visualização");

        assertThat(role.getDescription()).contains("á", "õ", "ã", "ç");
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setName(RoleEnum.ADMIN);
        role.setDescription("Admin original");

        Long originalId = role.getId();

        role.setDescription("Admin atualizado");

        assertThat(role.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support different role types")
    void shouldSupportDifferentRoleTypes() {

        RoleEntity adminRole = new RoleEntity();
        adminRole.setName(RoleEnum.ADMIN);
        adminRole.setDescription("Administrador do sistema");

        RoleEntity collaboratorRole = new RoleEntity();
        collaboratorRole.setName(RoleEnum.COLLABORATOR);
        collaboratorRole.setDescription("Colaborador do sistema");

        assertThat(adminRole.getName()).isNotEqualTo(collaboratorRole.getName());
        assertThat(adminRole.getDescription()).isNotEqualTo(collaboratorRole.getDescription());
    }

    @Test
    @DisplayName("Should support unique name constraint concept")
    void shouldSupportUniqueNameConstraintConcept() {

        RoleEntity role1 = new RoleEntity();
        role1.setId(1L);
        role1.setName(RoleEnum.ADMIN);

        RoleEntity role2 = new RoleEntity();
        role2.setId(2L);
        role2.setName(RoleEnum.COLLABORATOR);

        assertThat(role1.getName()).isNotEqualTo(role2.getName());
    }

    @Test
    @DisplayName("Should support system roles hierarchy concept")
    void shouldSupportSystemRolesHierarchyConcept() {

        RoleEntity admin = new RoleEntity(
                RoleEnum.ADMIN,
                "Acesso total ao sistema"
        );

        RoleEntity collaborator = new RoleEntity(
                RoleEnum.COLLABORATOR,
                "Acesso limitado ao sistema"
        );

        assertThat(admin.getName()).isEqualTo(RoleEnum.ADMIN);
        assertThat(collaborator.getName()).isEqualTo(RoleEnum.COLLABORATOR);
    }
}
