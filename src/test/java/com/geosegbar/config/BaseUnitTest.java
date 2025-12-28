package com.geosegbar.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Classe base para testes unitários.
 *
 * Esta classe fornece configurações comuns para todos os testes unitários,
 * incluindo a integração com Mockito para criação de mocks.
 *
 * Características: - Não carrega o contexto Spring (mais rápido) - Usa apenas
 * Mockito para mocks - Ideal para testar lógica de negócio isolada
 *
 * Uso: public class MyServiceTest extends BaseUnitTest {
 *
 * @Mock private MyRepository repository;
 *
 * @InjectMocks private MyService service; }
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
public abstract class BaseUnitTest {

    @BeforeEach
    void baseSetUp() {
        // Setup comum para todos os testes unitários
        // Pode ser sobrescrito pelas classes filhas
    }

    /**
     * Aguarda um tempo determinado (útil para testes assíncronos).
     *
     * @param milliseconds tempo em milissegundos
     */
    protected void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        }
    }
}
