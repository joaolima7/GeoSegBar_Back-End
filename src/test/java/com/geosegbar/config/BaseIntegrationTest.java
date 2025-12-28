package com.geosegbar.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Classe base para testes de integração.
 *
 * Esta classe fornece configurações comuns para testes de integração, incluindo
 * containers Docker para PostgreSQL e Redis usando Testcontainers.
 *
 * Características: - Carrega contexto Spring completo - Usa Testcontainers para
 * PostgreSQL e Redis - Containers são singleton (reutilizados entre testes) -
 * Profile 'test' ativado automaticamente
 *
 * Uso:
 *
 * @DataJpaTest public class MyRepositoryIT extends BaseIntegrationTest {
 * @Autowired private MyRepository repository; }
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
public abstract class BaseIntegrationTest {

    // PostgreSQL Container (singleton pattern)
    private static final PostgreSQLContainer<?> postgresContainer;

    // Redis Container (singleton pattern)
    private static final GenericContainer<?> redisContainer;

    static {
        // Inicializa PostgreSQL container
        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("geosegbar_test")
                .withUsername("test")
                .withPassword("test")
                .withReuse(true); // Reutiliza o container entre execuções

        // Inicializa Redis container
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true); // Reutiliza o container entre execuções
    }

    @BeforeAll
    static void startContainers() {
        if (!postgresContainer.isRunning()) {
            postgresContainer.start();
        }
        if (!redisContainer.isRunning()) {
            redisContainer.start();
        }
    }

    @AfterAll
    static void stopContainers() {
        // Não para os containers aqui devido ao reuse
        // Eles serão parados automaticamente ao final dos testes
    }

    /**
     * Configura propriedades dinâmicas do Spring baseadas nos containers. Este
     * método é chamado automaticamente pelo Spring Test.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configura PostgreSQL
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Configura JPA
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
        registry.add("spring.jpa.properties.hibernate.format_sql", () -> "false");

        // Configura Redis
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        // Desabilita cache para testes (opcional)
        registry.add("spring.cache.type", () -> "none");
    }

    /**
     * Retorna o container PostgreSQL (útil para operações customizadas).
     */
    protected static PostgreSQLContainer<?> getPostgresContainer() {
        return postgresContainer;
    }

    /**
     * Retorna o container Redis (útil para operações customizadas).
     */
    protected static GenericContainer<?> getRedisContainer() {
        return redisContainer;
    }
}
