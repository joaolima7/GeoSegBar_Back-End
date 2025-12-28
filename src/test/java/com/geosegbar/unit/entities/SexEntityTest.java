package com.geosegbar.unit.entities;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.SexEntity;
import com.geosegbar.entities.UserEntity;

@Tag("unit")
class SexEntityTest extends BaseUnitTest {

    @Test
    @DisplayName("Should create sex with all required fields")
    void shouldCreateSexWithAllRequiredFields() {

        SexEntity sex = new SexEntity();
        sex.setId(1L);
        sex.setName("Masculino");

        assertThat(sex).satisfies(s -> {
            assertThat(s.getId()).isEqualTo(1L);
            assertThat(s.getName()).isEqualTo("Masculino");
        });
    }

    @Test
    @DisplayName("Should create using all args constructor")
    void shouldCreateUsingAllArgsConstructor() {

        SexEntity sex = new SexEntity(1L, "Feminino", null);

        assertThat(sex.getId()).isEqualTo(1L);
        assertThat(sex.getName()).isEqualTo("Feminino");
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of users")
    void shouldMaintainOneToManyCollectionOfUsers() {

        SexEntity sex = new SexEntity();
        sex.setName("Masculino");

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setSex(sex);

        sex.getUsers().add(user);

        assertThat(sex.getUsers())
                .isNotNull()
                .hasSize(1)
                .contains(user);
    }

    @Test
    @DisplayName("Should initialize empty users collection by default")
    void shouldInitializeEmptyUsersCollectionByDefault() {

        SexEntity sex = new SexEntity();

        assertThat(sex.getUsers()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should support multiple users per sex")
    void shouldSupportMultipleUsersPerSex() {

        SexEntity sex = new SexEntity();
        sex.setName("Feminino");

        UserEntity user1 = new UserEntity();
        user1.setId(1L);

        UserEntity user2 = new UserEntity();
        user2.setId(2L);

        UserEntity user3 = new UserEntity();
        user3.setId(3L);

        sex.getUsers().add(user1);
        sex.getUsers().add(user2);
        sex.getUsers().add(user3);

        assertThat(sex.getUsers()).hasSize(3);
    }

    @Test
    @DisplayName("Should support common sex value Masculino")
    void shouldSupportCommonSexValueMasculino() {

        SexEntity sex = new SexEntity();
        sex.setName("Masculino");

        assertThat(sex.getName()).isEqualTo("Masculino");
    }

    @Test
    @DisplayName("Should support common sex value Feminino")
    void shouldSupportCommonSexValueFeminino() {

        SexEntity sex = new SexEntity();
        sex.setName("Feminino");

        assertThat(sex.getName()).isEqualTo("Feminino");
    }

    @Test
    @DisplayName("Should support common sex value Outro")
    void shouldSupportCommonSexValueOutro() {

        SexEntity sex = new SexEntity();
        sex.setName("Outro");

        assertThat(sex.getName()).isEqualTo("Outro");
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        SexEntity sex = new SexEntity();
        sex.setName("Não Informado");

        assertThat(sex.getName()).contains("ã");
    }

    @Test
    @DisplayName("Should support descriptive sex names")
    void shouldSupportDescriptiveSexNames() {

        SexEntity sex = new SexEntity();
        sex.setName("Prefiro não informar");

        assertThat(sex.getName()).hasSize(20);
    }

    @Test
    @DisplayName("Should support short sex names")
    void shouldSupportShortSexNames() {

        SexEntity sex = new SexEntity();
        sex.setName("M");

        assertThat(sex.getName()).hasSize(1);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        SexEntity sex = new SexEntity();
        sex.setId(1L);
        sex.setName("Masculino");

        Long originalId = sex.getId();

        sex.setName("Feminino");

        assertThat(sex.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support bidirectional relationship with users")
    void shouldSupportBidirectionalRelationshipWithUsers() {

        SexEntity sex = new SexEntity();
        sex.setId(1L);
        sex.setName("Masculino");

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setSex(sex);

        sex.getUsers().add(user);

        assertThat(user.getSex()).isEqualTo(sex);
        assertThat(sex.getUsers()).contains(user);
    }

    @Test
    @DisplayName("Should support adding and removing users")
    void shouldSupportAddingAndRemovingUsers() {

        SexEntity sex = new SexEntity();
        UserEntity user = new UserEntity();
        user.setId(1L);

        sex.getUsers().add(user);

        assertThat(sex.getUsers()).hasSize(1);

        sex.getUsers().remove(user);

        assertThat(sex.getUsers()).isEmpty();
    }
}
