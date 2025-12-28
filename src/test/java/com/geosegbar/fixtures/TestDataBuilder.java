package com.geosegbar.fixtures;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.geosegbar.common.enums.RoleEnum;
import com.geosegbar.common.enums.StatusEnum;
import com.geosegbar.entities.ClientEntity;
import com.geosegbar.entities.DamEntity;
import com.geosegbar.entities.RoleEntity;
import com.geosegbar.entities.SexEntity;
import com.geosegbar.entities.StatusEntity;
import com.geosegbar.entities.UserEntity;

/**
 * Classe base para criação de dados de teste (Test Data Builder).
 *
 * Esta classe fornece métodos auxiliares para criar objetos de domínio com
 * valores padrão válidos, facilitando a escrita de testes.
 *
 * Características: - Usa padrão Builder para construção fluente - Gera IDs
 * sequenciais automaticamente - Valores padrão válidos e consistentes - Métodos
 * para customização fácil
 *
 * Uso: UserEntity user = TestDataBuilder.user() .withName("João Silva")
 * .withEmail("joao@test.com") .build();
 */
public class TestDataBuilder {

    private static final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Gera próximo ID sequencial (útil para testes unitários).
     */
    public static Long nextId() {
        return idGenerator.getAndIncrement();
    }

    /**
     * Reseta o gerador de IDs (útil entre testes).
     */
    public static void resetIdGenerator() {
        idGenerator.set(1);
    }

    // =========================================================================
    // STATUS
    // =========================================================================
    public static StatusEntity activeStatus() {
        StatusEntity status = new StatusEntity();
        status.setId(1L);
        status.setStatus(StatusEnum.ACTIVE);
        return status;
    }

    public static StatusEntity disabledStatus() {
        StatusEntity status = new StatusEntity();
        status.setId(2L);
        status.setStatus(StatusEnum.DISABLED);
        return status;
    }

    // =========================================================================
    // ROLE
    // =========================================================================
    public static RoleEntity adminRole() {
        RoleEntity role = new RoleEntity();
        role.setId(1L);
        role.setName(RoleEnum.ADMIN);
        role.setDescription("Administrador do sistema");
        return role;
    }

    public static RoleEntity collaboratorRole() {
        RoleEntity role = new RoleEntity();
        role.setId(2L);
        role.setName(RoleEnum.COLLABORATOR);
        role.setDescription("Colaborador");
        return role;
    }

    // Removido: MANAGER não existe no RoleEnum do projeto
    // =========================================================================
    // SEX
    // =========================================================================
    public static SexEntity maleSex() {
        SexEntity sex = new SexEntity();
        sex.setId(1L);
        sex.setName("Masculino");
        return sex;
    }

    public static SexEntity femaleSex() {
        SexEntity sex = new SexEntity();
        sex.setId(2L);
        sex.setName("Feminino");
        return sex;
    }

    // =========================================================================
    // USER BUILDER
    // =========================================================================
    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static class UserBuilder {

        private final UserEntity user = new UserEntity();

        public UserBuilder() {
            // Valores padrão
            user.setId(nextId());
            user.setName("Test User");
            user.setEmail("test" + user.getId() + "@geosegbar.com");
            user.setPassword("$2a$10$testHashedPassword"); // BCrypt hash simulado
            user.setPhone("11999999999");
            user.setSex(maleSex());
            user.setStatus(activeStatus());
            user.setRole(collaboratorRole());
            user.setIsFirstAccess(false);
            user.setClients(new HashSet<>());
        }

        public UserBuilder withId(Long id) {
            user.setId(id);
            return this;
        }

        public UserBuilder withName(String name) {
            user.setName(name);
            return this;
        }

        public UserBuilder withEmail(String email) {
            user.setEmail(email);
            return this;
        }

        public UserBuilder withPassword(String password) {
            user.setPassword(password);
            return this;
        }

        public UserBuilder withPhone(String phone) {
            user.setPhone(phone);
            return this;
        }

        public UserBuilder withSex(SexEntity sex) {
            user.setSex(sex);
            return this;
        }

        public UserBuilder withStatus(StatusEntity status) {
            user.setStatus(status);
            return this;
        }

        public UserBuilder withRole(RoleEntity role) {
            user.setRole(role);
            return this;
        }

        public UserBuilder withIsFirstAccess(Boolean isFirstAccess) {
            user.setIsFirstAccess(isFirstAccess);
            return this;
        }

        public UserBuilder withClients(Set<ClientEntity> clients) {
            user.setClients(clients);
            return this;
        }

        public UserBuilder withCreatedBy(UserEntity createdBy) {
            user.setCreatedBy(createdBy);
            return this;
        }

        public UserBuilder asAdmin() {
            user.setRole(adminRole());
            return this;
        }

        public UserBuilder asCollaborator() {
            user.setRole(collaboratorRole());
            return this;
        }

        public UserEntity build() {
            return user;
        }
    }

    // =========================================================================
    // CLIENT BUILDER
    // =========================================================================
    public static ClientBuilder client() {
        return new ClientBuilder();
    }

    public static class ClientBuilder {

        private final ClientEntity client = new ClientEntity();

        public ClientBuilder() {
            // Valores padrão
            Long id = nextId();
            client.setId(id);
            client.setName("Test Client " + id);
            client.setEmail("client" + id + "@geosegbar.com");
            client.setStreet("Rua Test");
            client.setNeighborhood("Bairro Test");
            client.setNumberAddress("123");
            client.setCity("São Paulo");
            client.setState("São Paulo");
            client.setZipCode("01310-100");
            client.setPhone("11988887777");
            client.setStatus(activeStatus());
        }

        public ClientBuilder withId(Long id) {
            client.setId(id);
            return this;
        }

        public ClientBuilder withName(String name) {
            client.setName(name);
            return this;
        }

        public ClientBuilder withEmail(String email) {
            client.setEmail(email);
            return this;
        }

        public ClientBuilder withCity(String city) {
            client.setCity(city);
            return this;
        }

        public ClientBuilder withState(String state) {
            client.setState(state);
            return this;
        }

        public ClientBuilder withStatus(StatusEntity status) {
            client.setStatus(status);
            return this;
        }

        public ClientEntity build() {
            return client;
        }
    }

    // =========================================================================
    // DAM BUILDER
    // =========================================================================
    public static DamBuilder dam() {
        return new DamBuilder();
    }

    public static class DamBuilder {

        private final DamEntity dam = new DamEntity();

        public DamBuilder() {
            // Valores padrão
            Long id = nextId();
            dam.setId(id);
            dam.setName("Barragem Test " + id);
            dam.setLatitude(-23.5505);
            dam.setLongitude(-46.6333);
            dam.setStreet("Avenida Test");
            dam.setNeighborhood("Centro");
            dam.setNumberAddress("100");
            dam.setCity("São Paulo");
            dam.setState("São Paulo");
            dam.setZipCode("01310-100");
            dam.setStatus(activeStatus());
        }

        public DamBuilder withId(Long id) {
            dam.setId(id);
            return this;
        }

        public DamBuilder withName(String name) {
            dam.setName(name);
            return this;
        }

        public DamBuilder withClient(ClientEntity client) {
            dam.setClient(client);
            return this;
        }

        public DamBuilder withLatitude(Double latitude) {
            dam.setLatitude(latitude);
            return this;
        }

        public DamBuilder withLongitude(Double longitude) {
            dam.setLongitude(longitude);
            return this;
        }

        public DamBuilder withCity(String city) {
            dam.setCity(city);
            return this;
        }

        public DamBuilder withState(String state) {
            dam.setState(state);
            return this;
        }

        public DamBuilder withStatus(StatusEntity status) {
            dam.setStatus(status);
            return this;
        }

        public DamEntity build() {
            return dam;
        }
    }
}
