package com.geosegbar.unit.exceptions;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.geosegbar.common.email.EmailService;
import com.geosegbar.config.BaseUnitTest;
import com.geosegbar.exceptions.exception_handler.RestExceptionHandler;
import com.geosegbar.infra.instrument_graph_axes.dtos.UpdateGraphAxesRequestDTO;
import com.geosegbar.infra.instrument_graph_axes.services.InstrumentGraphAxesService;
import com.geosegbar.infra.instrument_graph_axes.web.InstrumentGraphAxesController;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Corpo ilegível é erro de quem chamou: 400, sem e-mail de erro interno.
 * O caso que motivou: PUT /graph-axes/pattern/306 com "346,5" em campo Double.
 */
@Tag("unit")
@DisplayName("Unit Tests - Corpo ilegível responde 400")
class UnreadableBodyHandlerTest extends BaseUnitTest {

    private final EmailService emailService = mock(EmailService.class);
    private final InstrumentGraphAxesService axesService = mock(InstrumentGraphAxesService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUpMvc() {
        RestExceptionHandler handler = new RestExceptionHandler();
        ReflectionTestUtils.setField(handler, "emailService", emailService);
        mvc = MockMvcBuilders.standaloneSetup(new InstrumentGraphAxesController(axesService))
                .setControllerAdvice(handler)
                .build();
    }

    private ResultActions send(String body) throws Exception {
        return mvc.perform(put("/graph-axes/pattern/306").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String axes(String maximum) {
        return """
                {"abscissaPx":12,"abscissaGridLinesEnable":true,"primaryOrdinatePx":12,
                 "secondaryOrdinatePx":12,"primaryOrdinateGridLinesEnable":true,
                 "primaryOrdinateMaximumValue":%s}""".formatted(maximum);
    }

    @Test
    @DisplayName("Decimal com vírgula vira 400 com o nome do campo, sem e-mail")
    void commaDecimalIs400() throws Exception {
        send(axes("\"346,5\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Valor inválido para o campo 'primaryOrdinateMaximumValue'."));
        verifyNoInteractions(emailService, axesService);
    }

    @Test
    @DisplayName("JSON malformado vira 400, sem e-mail")
    void malformedJsonIs400() throws Exception {
        send("{").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(emailService, axesService);
    }

    @Test
    @DisplayName("Corpo vazio vira 400, sem e-mail")
    void emptyBodyIs400() throws Exception {
        send("").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Corpo da requisição inválido."));
        verifyNoInteractions(emailService, axesService);
    }

    @Test
    @DisplayName("Decimal numérico chega ao serviço")
    void numericDecimalIsAccepted() throws Exception {
        send(axes("346.5")).andExpect(status().isOk());
        verify(axesService).updateAxes(eq(306L), any(UpdateGraphAxesRequestDTO.class));
    }
}
