package com.geosegbar.infra.reading.dtos;

import com.geosegbar.common.enums.LimitStatusEnum;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstrumentLimitStatusDTO {

    private Long instrumentId;
    private String instrumentName;
    private String instrumentType;
    private Long instrumentTypeId;
    private Long damId;
    private String damName;
    private Long clientId;
    private String clientName;
    private LimitStatusEnum limitStatus;
    private String lastReadingDate;
}
