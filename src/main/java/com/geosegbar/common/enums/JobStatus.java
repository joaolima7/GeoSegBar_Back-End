package com.geosegbar.common.enums;

/**
 * Status do job de coleta de dados históricos
 */
public enum JobStatus {
    /**
     * Job foi criado e está na fila aguardando processamento
     */
    QUEUED,
    /**
     * Job está sendo processado por um worker
     */
    PROCESSING,
    /**
     * Job foi completado com sucesso - todos os dados foram coletados
     */
    COMPLETED,
    /**
     * Job falhou após 3 tentativas de retry - requer intervenção manual
     */
    FAILED,
    /**
     * Job pausado temporariamente devido a erro recuperável - será retentado
     */
    PAUSED
}
