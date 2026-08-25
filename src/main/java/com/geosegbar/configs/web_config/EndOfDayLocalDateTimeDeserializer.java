package com.geosegbar.configs.web_config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Igual ao {@link LenientLocalDateTimeDeserializer}, exceto num ponto: data pura
 * vira o FIM do dia, não o começo.
 *
 * Use em campo de expiração ou de limite superior de intervalo.
 *
 * POR QUE ISSO IMPORTA
 *   O usuário escolhe "26/08/2026" como data de validade do link entendendo que
 *   ele funciona durante o dia 26. Interpretar como 26/08 00:00:00 faria o link
 *   nascer vencido: qualquer acesso depois da meia-noite já estaria expirado, e
 *   a pessoa receberia "este link expirou" no mesmo dia em que foi criado.
 *
 *   Com o fim do dia, "expira em 26/08" significa o que qualquer pessoa entende:
 *   vale até o último instante do dia 26.
 */
public class EndOfDayLocalDateTimeDeserializer extends LenientLocalDateTimeDeserializer {

    @Override
    protected LocalDateTime aoReceberDataPura(LocalDate data) {
        return data.atTime(LocalTime.MAX.withNano(0));
    }
}
