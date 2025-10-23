package com.geosegbar.infra.client.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientStatusUpdateDTO {

    @NotNull(message = "ID do status é obrigatório")
    private Long statusId;
}
