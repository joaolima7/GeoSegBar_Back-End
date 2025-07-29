package com.geosegbar.infra.reading.dtos;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class ImportReadingsResult {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<String> errors = new ArrayList<>();
}
