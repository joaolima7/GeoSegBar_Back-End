package com.geosegbar.infra.reading.dtos;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkToggleActiveResponseDTO {

    private List<Long> successfulIds;
    private List<FailedOperation> failedOperations;
    private int totalProcessed;
    private int successCount;
    private int failureCount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedOperation {

        private Long readingId;
        private String errorMessage;
    }
}
