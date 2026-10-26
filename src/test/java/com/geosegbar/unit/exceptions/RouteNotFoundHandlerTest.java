package com.geosegbar.unit.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.exceptions.exception_handler.RestExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rota inexistente responde 404 sem tratar como erro interno.
 *
 * Com spring.web.resources.add-mappings=false o DispatcherServlet lança
 * NoHandlerFoundException para caminho sem controller. Sem tratamento próprio
 * ela caía no handler genérico: 500, stack trace e e-mail de erro interno. O
 * caso que motivou: /actuator/* na porta da API (9090), depois que o actuator
 * foi para a porta de management (8081), coletado a cada 30 s por um Prometheus
 * antigo.
 */
@Tag("unit")
@DisplayName("Unit Tests - Rota inexistente responde 404")
class RouteNotFoundHandlerTest extends BaseUnitTest {

    @RestController
    static class AlgumController {

        @GetMapping("/existe")
        String existe() {
            return "ok";
        }
    }

    @Test
    @DisplayName("NoHandlerFoundException vira 404 e não manda e-mail de erro")
    void noHandlerFoundIs404() {
        EmailService emailService = mock(EmailService.class);
        RestExceptionHandler handler = new RestExceptionHandler();
        ReflectionTestUtils.setField(handler, "emailService", emailService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/prometheus");

        var response = handler.handleNoResourceFoundException(
                new NoHandlerFoundException("GET", "/actuator/prometheus", new HttpHeaders()), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Pelo DispatcherServlet, /actuator/prometheus na porta da API dá 404")
    void actuatorOnApiPortIs404() throws Exception {
        EmailService emailService = mock(EmailService.class);
        RestExceptionHandler handler = new RestExceptionHandler();
        ReflectionTestUtils.setField(handler, "emailService", emailService);

        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AlgumController())
                .setControllerAdvice(handler)
                .build();

        mvc.perform(get("/actuator/prometheus")).andExpect(status().isNotFound());
        verifyNoInteractions(emailService);
    }
}
