package com.geosegbar.infra.instrument_type.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentTypeDTO {

    private Long id;

    @NotBlank(message = "Nome do tipo de instrumento é obrigatório")
    private String name;

    /**
     * Cliente dono do tipo. Obrigatório na criação; ignorado na edição, porque
     * mudar o tipo de cliente moveria instrumentos de barragens já cadastradas.
     * Nulo apenas em tipos legados ainda não migrados.
     */
    private Long clientId;

    /**
     * Somente leitura.
     */
    private String clientName;

    /**
     * Quantos instrumentos usam este tipo. Zero significa que o tipo pode ser
     * excluído; acima de zero a exclusão é recusada.
     */
    private Long instrumentsCount;

    /**
     * Em quantas barragens do cliente o tipo está em uso. Serve para o aviso de
     * impacto antes de renomear — a alteração reflete em todas elas.
     */
    private Long damsCount;

    /**
     * true quando o tipo ainda não foi atrelado a um cliente. Nesse estado ele
     * continua funcionando nos instrumentos existentes, mas é somente leitura.
     */
    private Boolean legacy;
}
