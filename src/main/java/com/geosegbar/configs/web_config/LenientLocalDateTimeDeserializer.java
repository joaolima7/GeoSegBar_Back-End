package com.geosegbar.configs.web_config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Lê datas em qualquer formato que o front costume mandar.
 *
 * O padrão do Jackson só aceita ISO completo. Um campo de data preenchido por
 * date picker chega como "2026-08-26", e a requisição inteira falhava com
 * DateTimeParseException — foi o que quebrou o compartilhamento de PSB em
 * 25/08/2026: o usuário escolhia a data de expiração e recebia erro genérico.
 *
 * Formatos aceitos:
 *
 *   2026-08-26                     data pura
 *   2026-08-26T14:30               ISO sem segundos
 *   2026-08-26T14:30:00            ISO
 *   2026-08-26T14:30:00.123        ISO com fração
 *   2026-08-26 14:30:00            espaço no lugar do T
 *   2026-08-26T14:30:00Z           instante UTC
 *   2026-08-26T14:30:00-03:00      instante com deslocamento
 *   26/08/2026                     formato brasileiro
 *   26/08/2026 14:30:00            formato brasileiro com hora
 *   1787601575000                  epoch em milissegundos
 *   ""                             string vazia vira null
 *
 * Instantes com fuso (Z ou deslocamento) são convertidos para o fuso da
 * aplicação — America/Sao_Paulo. Guardar o horário "cru" faria uma data enviada
 * como 23:59Z virar 23:59 local, três horas adiantada.
 *
 * DATA PURA VIRA INÍCIO DO DIA. Para um campo de expiração isso está errado —
 * veja {@link EndOfDayLocalDateTimeDeserializer}.
 */
public class LenientLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    /**
     * Ordem importa: o primeiro que casar vence. Os mais específicos vêm antes.
     */
    private static final List<DateTimeFormatter> FORMATOS_COM_HORA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    );

    /**
     * Formatos de data pura. dd/MM/yyyy é a convenção brasileira — este sistema
     * atende barragens no Brasil, então 03/04/2026 é 3 de abril, nunca 4 de março.
     */
    private static final List<DateTimeFormatter> FORMATOS_DATA_PURA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private static final ZoneId FUSO_APLICACAO = ZoneId.of("America/Sao_Paulo");

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        // Epoch em milissegundos, quando o front manda número em vez de texto.
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(p.getLongValue()), FUSO_APLICACAO);
        }

        String texto = p.getText();
        if (texto == null || texto.isBlank()) {
            // Campo limpo no formulário chega como "" — é ausência, não erro.
            return null;
        }
        texto = texto.trim();

        // Instante com fuso explícito: converte para o fuso da aplicação.
        if (texto.endsWith("Z") || temDeslocamento(texto)) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(texto), FUSO_APLICACAO);
            } catch (DateTimeParseException ignorado) {
                try {
                    return java.time.OffsetDateTime.parse(texto)
                            .atZoneSameInstant(FUSO_APLICACAO)
                            .toLocalDateTime();
                } catch (DateTimeParseException tambemIgnorado) {
                    // Cai nos formatos abaixo.
                }
            }
        }

        for (DateTimeFormatter formato : FORMATOS_COM_HORA) {
            try {
                return LocalDateTime.parse(texto, formato);
            } catch (DateTimeParseException ignorado) {
                // tenta o próximo
            }
        }

        for (DateTimeFormatter formato : FORMATOS_DATA_PURA) {
            try {
                return aoReceberDataPura(LocalDate.parse(texto, formato));
            } catch (DateTimeParseException ignorado) {
                // tenta o próximo
            }
        }

        throw new IOException(
                "Data em formato não reconhecido: \"" + texto + "\". "
                + "Use AAAA-MM-DD, DD/MM/AAAA ou o formato ISO completo.");
    }

    /**
     * O que fazer quando chega só a data, sem hora. Subclasses ajustam conforme
     * o significado do campo.
     */
    protected LocalDateTime aoReceberDataPura(LocalDate data) {
        return data.atStartOfDay();
    }

    /**
     * Detecta deslocamento de fuso (+03:00 / -03:00) sem confundir com o hífen
     * separador da data.
     */
    private boolean temDeslocamento(String texto) {
        int posicaoT = texto.indexOf('T');
        if (posicaoT < 0) {
            return false;
        }
        String parteHora = texto.substring(posicaoT);
        return parteHora.contains("+") || parteHora.contains("-");
    }
}
