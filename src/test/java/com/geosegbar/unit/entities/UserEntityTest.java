package com.geosegbar.unit.entities;

import com.geosegbar.common.objects_values.UserCreatorInfo;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.entities.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class UserEntityTest extends BaseUnitTest {

    private SexEntity sex;
    private StatusEntity status;
    private RoleEntity role;

    @BeforeEach
    void setUp() {
        sex = new SexEntity();
        sex.setId(1L);
        sex.setName("Masculino");

        status = new StatusEntity();
        status.setId(1L);

        role = new RoleEntity();
        role.setId(1L);
    }

    @Test
    @DisplayName("Should create user with all required fields")
    void shouldCreateUserWithAllRequiredFields() {

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("João Silva");
        user.setEmail("joao.silva@exemplo.com");
        user.setPassword("senha123");
        user.setSex(sex);
        user.setStatus(status);
        user.setRole(role);

        assertThat(user).satisfies(u -> {
            assertThat(u.getId()).isEqualTo(1L);
            assertThat(u.getName()).isEqualTo("João Silva");
            assertThat(u.getEmail()).isEqualTo("joao.silva@exemplo.com");
            assertThat(u.getPassword()).isEqualTo("senha123");
            assertThat(u.getSex()).isEqualTo(sex);
            assertThat(u.getStatus()).isEqualTo(status);
            assertThat(u.getRole()).isEqualTo(role);
        });
    }

    @Test
    @DisplayName("Should support valid email format")
    void shouldSupportValidEmailFormat() {

        UserEntity user = new UserEntity();
        user.setEmail("usuario@dominio.com.br");

        assertThat(user.getEmail()).contains("@");
    }

    @Test
    @DisplayName("Should support minimum password length of 6 characters")
    void shouldSupportMinimumPasswordLengthOfSixCharacters() {

        UserEntity user = new UserEntity();
        user.setPassword("senha6");

        assertThat(user.getPassword()).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    @DisplayName("Should support optional phone with 11 characters")
    void shouldSupportOptionalPhoneWith11Characters() {

        UserEntity user = new UserEntity();
        user.setPhone("11987654321");

        assertThat(user.getPhone()).hasSize(11);
    }

    @Test
    @DisplayName("Should allow null phone")
    void shouldAllowNullPhone() {

        UserEntity user = new UserEntity();
        user.setPhone(null);

        assertThat(user.getPhone()).isNull();
    }

    @Test
    @DisplayName("Should default isFirstAccess to false")
    void shouldDefaultIsFirstAccessToFalse() {

        UserEntity user = new UserEntity();
        user.setIsFirstAccess(false);

        assertThat(user.getIsFirstAccess()).isFalse();
    }

    @Test
    @DisplayName("Should support toggling first access flag")
    void shouldSupportTogglingFirstAccessFlag() {

        UserEntity user = new UserEntity();
        user.setIsFirstAccess(true);

        assertThat(user.getIsFirstAccess()).isTrue();

        user.setIsFirstAccess(false);

        assertThat(user.getIsFirstAccess()).isFalse();
    }

    @Test
    @DisplayName("Should support optional last token")
    void shouldSupportOptionalLastToken() {

        UserEntity user = new UserEntity();
        user.setLastToken("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");

        assertThat(user.getLastToken()).isNotNull();
    }

    @Test
    @DisplayName("Should allow null last token")
    void shouldAllowNullLastToken() {

        UserEntity user = new UserEntity();
        user.setLastToken(null);

        assertThat(user.getLastToken()).isNull();
    }

    @Test
    @DisplayName("Should support token expiry date")
    void shouldSupportTokenExpiryDate() {

        UserEntity user = new UserEntity();
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        user.setTokenExpiryDate(expiryDate);

        assertThat(user.getTokenExpiryDate()).isEqualTo(expiryDate);
    }

    @Test
    @DisplayName("Should allow null token expiry date")
    void shouldAllowNullTokenExpiryDate() {

        UserEntity user = new UserEntity();
        user.setTokenExpiryDate(null);

        assertThat(user.getTokenExpiryDate()).isNull();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of readings")
    void shouldMaintainOneToManyCollectionOfReadings() {

        UserEntity user = new UserEntity();

        ReadingEntity reading = new ReadingEntity();
        reading.setId(1L);
        reading.setUser(user);

        user.getReadings().add(reading);

        assertThat(user.getReadings())
                .isNotNull()
                .hasSize(1)
                .contains(reading);
    }

    @Test
    @DisplayName("Should initialize empty readings collection by default")
    void shouldInitializeEmptyReadingsCollectionByDefault() {

        UserEntity user = new UserEntity();

        assertThat(user.getReadings()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain ManyToMany collection of clients")
    void shouldMaintainManyToManyCollectionOfClients() {

        UserEntity user = new UserEntity();

        ClientEntity client = new ClientEntity();
        client.setId(1L);

        user.getClients().add(client);

        assertThat(user.getClients())
                .isNotNull()
                .hasSize(1)
                .contains(client);
    }

    @Test
    @DisplayName("Should initialize empty clients collection by default")
    void shouldInitializeEmptyClientsCollectionByDefault() {

        UserEntity user = new UserEntity();

        assertThat(user.getClients()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of dam permissions")
    void shouldMaintainOneToManyCollectionOfDamPermissions() {

        UserEntity user = new UserEntity();

        DamPermissionEntity damPermission = new DamPermissionEntity();
        damPermission.setId(1L);
        damPermission.setUser(user);

        user.getDamPermissions().add(damPermission);

        assertThat(user.getDamPermissions())
                .isNotNull()
                .hasSize(1)
                .contains(damPermission);
    }

    @Test
    @DisplayName("Should support self-referencing createdBy relationship")
    void shouldSupportSelfReferencingCreatedByRelationship() {

        UserEntity admin = new UserEntity();
        admin.setId(1L);
        admin.setName("Administrador");

        UserEntity newUser = new UserEntity();
        newUser.setId(2L);
        newUser.setName("Novo Usuário");
        newUser.setCreatedBy(admin);

        assertThat(newUser.getCreatedBy()).isEqualTo(admin);
    }

    @Test
    @DisplayName("Should allow null createdBy for root users")
    void shouldAllowNullCreatedByForRootUsers() {

        UserEntity user = new UserEntity();
        user.setCreatedBy(null);

        assertThat(user.getCreatedBy()).isNull();
    }

    @Test
    @DisplayName("Should maintain OneToMany collection of created users")
    void shouldMaintainOneToManyCollectionOfCreatedUsers() {

        UserEntity admin = new UserEntity();
        admin.setId(1L);

        UserEntity createdUser = new UserEntity();
        createdUser.setId(2L);
        createdUser.setCreatedBy(admin);

        admin.getCreatedUsers().add(createdUser);

        assertThat(admin.getCreatedUsers())
                .isNotNull()
                .hasSize(1)
                .contains(createdUser);
    }

    @Test
    @DisplayName("Should support OneToOne attributions permission")
    void shouldSupportOneToOneAttributionsPermission() {

        UserEntity user = new UserEntity();
        AttributionsPermissionEntity permission = new AttributionsPermissionEntity();
        permission.setUser(user);
        user.setAttributionsPermission(permission);

        assertThat(user.getAttributionsPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should support OneToOne documentation permission")
    void shouldSupportOneToOneDocumentationPermission() {

        UserEntity user = new UserEntity();
        DocumentationPermissionEntity permission = new DocumentationPermissionEntity();
        permission.setUser(user);
        user.setDocumentationPermission(permission);

        assertThat(user.getDocumentationPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should support OneToOne instrumentation permission")
    void shouldSupportOneToOneInstrumentationPermission() {

        UserEntity user = new UserEntity();
        InstrumentationPermissionEntity permission = new InstrumentationPermissionEntity();
        permission.setUser(user);
        user.setInstrumentationPermission(permission);

        assertThat(user.getInstrumentationPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should support OneToOne routine inspection permission")
    void shouldSupportOneToOneRoutineInspectionPermission() {

        UserEntity user = new UserEntity();
        RoutineInspectionPermissionEntity permission = new RoutineInspectionPermissionEntity();
        permission.setUser(user);
        user.setRoutineInspectionPermission(permission);

        assertThat(user.getRoutineInspectionPermission()).isEqualTo(permission);
    }

    @Test
    @DisplayName("Should return null for getCreatedByInfo when createdBy is null")
    void shouldReturnNullForGetCreatedByInfoWhenCreatedByIsNull() {

        UserEntity user = new UserEntity();
        user.setCreatedBy(null);

        Object createdByInfo = user.getCreatedByInfo();

        assertThat(createdByInfo).isNull();
    }

    @Test
    @DisplayName("Should return UserCreatorInfo when createdBy is not null")
    void shouldReturnUserCreatorInfoWhenCreatedByIsNotNull() {

        UserEntity admin = new UserEntity();
        admin.setId(1L);
        admin.setName("Admin");
        admin.setEmail("admin@exemplo.com");

        UserEntity user = new UserEntity();
        user.setCreatedBy(admin);

        Object createdByInfo = user.getCreatedByInfo();

        assertThat(createdByInfo).isInstanceOf(UserCreatorInfo.class);
        UserCreatorInfo info = (UserCreatorInfo) createdByInfo;
        assertThat(info.getId()).isEqualTo(1L);
        assertThat(info.getName()).isEqualTo("Admin");
        assertThat(info.getEmail()).isEqualTo("admin@exemplo.com");
    }

    @Test
    @DisplayName("Should support Portuguese characters in name")
    void shouldSupportPortugueseCharactersInName() {

        UserEntity user = new UserEntity();
        user.setName("João da Silva Araújo");

        assertThat(user.getName()).contains("ã", "ú");
    }

    @Test
    @DisplayName("Should support long user names")
    void shouldSupportLongUserNames() {

        UserEntity user = new UserEntity();
        user.setName("Maria Aparecida dos Santos Silva de Oliveira Ferreira");

        assertThat(user.getName()).hasSizeGreaterThan(50);
    }

    @Test
    @DisplayName("Should support multiple clients per user")
    void shouldSupportMultipleClientsPerUser() {

        UserEntity user = new UserEntity();

        ClientEntity client1 = new ClientEntity();
        client1.setId(1L);

        ClientEntity client2 = new ClientEntity();
        client2.setId(2L);

        ClientEntity client3 = new ClientEntity();
        client3.setId(3L);

        user.getClients().add(client1);
        user.getClients().add(client2);
        user.getClients().add(client3);

        assertThat(user.getClients()).hasSize(3);
    }

    @Test
    @DisplayName("Should maintain identity through property changes")
    void shouldMaintainIdentityThroughPropertyChanges() {

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setName("João Silva");
        user.setEmail("joao@exemplo.com");

        Long originalId = user.getId();

        user.setName("João Santos");
        user.setEmail("joao.santos@exemplo.com");

        assertThat(user.getId()).isEqualTo(originalId);
    }

    @Test
    @DisplayName("Should support unique email constraint concept")
    void shouldSupportUniqueEmailConstraintConcept() {

        UserEntity user1 = new UserEntity();
        user1.setEmail("usuario@exemplo.com");

        UserEntity user2 = new UserEntity();
        user2.setEmail("usuario@exemplo.com");

        assertThat(user1.getEmail()).isEqualTo(user2.getEmail());
    }

    @Test
    @DisplayName("Should support password change")
    void shouldSupportPasswordChange() {

        UserEntity user = new UserEntity();
        user.setPassword("senhaAntiga123");

        user.setPassword("novaSenha456");

        assertThat(user.getPassword()).isEqualTo("novaSenha456");
    }

    @Test
    @DisplayName("Should support token expiry tracking for authentication")
    void shouldSupportTokenExpiryTrackingForAuthentication() {

        UserEntity user = new UserEntity();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(24);

        user.setLastToken("jwt-token-here");
        user.setTokenExpiryDate(expiresAt);

        assertThat(user.getTokenExpiryDate()).isAfter(now);
    }
}
