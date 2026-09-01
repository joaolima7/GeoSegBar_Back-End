package com.geosegbar.infra.checklist_response.dtos;

import java.time.LocalDateTime;

/**
 * Última inspeção de cada barragem — versão sem sentinela no campo de data.
 *
 * A v1 devolve lastChecklistDate como String "yyyy-MM-dd HH:mm:ss" e, quando a
 * barragem nunca foi inspecionada, coloca o texto "Nenhuma inspeção
 * realizada." NO MESMO CAMPO. O consumidor tenta parsear, falha, e cai numa
 * comparação de string que quebra se o texto mudar ou trocar de acento.
 *
 * Aqui a ausência é null, que é o que ausência sempre foi. O texto amigável
 * fica com quem desenha a tela.
 *
 * A v1 continua no ar, intacta e sem prazo para morrer: mudar o tipo de um
 * campo quebra qualquer consumidor, e a web usa aquela rota.
 */
public record DamLastChecklistV2DTO(
        Long damId,
        String damName,
        LocalDateTime lastChecklistDate) {

}
