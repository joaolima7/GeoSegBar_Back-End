package com.geosegbar.configs.web_config;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Contraparte do {@link LenientLocalDateTimeDeserializer} para campos de data
 * sem hora.
 *
 * O problema aqui é o espelho do outro: um front que manda
 * "2026-08-26T00:00:00.000Z" — comportamento padrão de vários componentes de
 * calendário — quebra um campo LocalDate, do mesmo jeito que "2026-08-26"
 * quebrava um LocalDateTime.
 *
 * São 12 DTOs com LocalDate no projeto, entre eles leitura de instrumento, PAE e
 * cadastro de barragem. Tratar só o LocalDateTime deixaria metade da exposição
 * de pé.
 *
 * Formatos aceitos:
 *
 *   2026-08-26                     ISO
 *   26/08/2026                     formato brasileiro
 *   2026-08-26T14:30:00            data e hora — a hora é descartada
 *   2026-08-26T00:00:00.000Z       instante — convertido ao fuso e truncado
 *   2026-08-26 14:30:00            espaço no lugar do T
 *   1787601575000                  epoch em milissegundos
 *   ""                             string vazia vira null
 */
public class LenientLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private static final ZoneId FUSO_APLICACAO = ZoneId.of("America/Sao_Paulo");

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {

        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(p.getLongValue()), FUSO_APLICACAO);
        }

        String texto = p.getText();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        texto = texto.trim();

        for (DateTimeFormatter formato : FORMATOS) {
            try {
                return LocalDate.parse(texto, formato);
            } catch (DateTimeParseException ignorado) {
                // tenta o próximo
            }
        }

        // Veio data e hora num campo que só quer a data: aproveita a parte da
        // data em vez de recusar. Reusa a tolerância já implementada para
        // LocalDateTime, então todo formato aceito lá vale aqui também.
        try {
            return new LenientLocalDateTimeDeserializer().deserialize(p, ctxt).toLocalDate();
        } catch (IOException | RuntimeException ignorado) {
            throw new IOException(
                    "Data em formato não reconhecido: \"" + texto + "\". "
                    + "Use AAAA-MM-DD ou DD/MM/AAAA.");
        }
    }
}
