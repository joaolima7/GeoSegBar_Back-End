package com.geosegbar.configs.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class IndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public IndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void createIndices() {
        log.info("Iniciando criação de índices em tabelas de junção...");

        String[] indexCommands = {
            "CREATE INDEX IF NOT EXISTS idx_checklist_template_checklist_id ON checklist_template_questionnaire(checklist_id)",
            "CREATE INDEX IF NOT EXISTS idx_checklist_template_questionnaire_id ON checklist_template_questionnaire(template_questionnaire_id)",
            "CREATE INDEX IF NOT EXISTS idx_checklist_dam_checklist_id ON checklist_dam(checklist_id)",
            "CREATE INDEX IF NOT EXISTS idx_checklist_dam_dam_id ON checklist_dam(dam_id)",
            "CREATE INDEX IF NOT EXISTS idx_question_option_question_id ON question_option(question_id)",
            "CREATE INDEX IF NOT EXISTS idx_question_option_option_id ON question_option(option_id)",
            "CREATE INDEX IF NOT EXISTS idx_answer_options_answer_id ON answer_options(answer_id)",
            "CREATE INDEX IF NOT EXISTS idx_answer_options_option_id ON answer_options(option_id)",
            "CREATE INDEX IF NOT EXISTS idx_ck_template_checklist_id ON checklist_template_questionnaire(checklist_id)",
            "CREATE INDEX IF NOT EXISTS idx_ck_template_template_id ON checklist_template_questionnaire(template_questionnaire_id)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_photo_path ON anomaly_photos(image_path)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_geo_bounds ON anomalies(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_dam_status_created ON anomalies(dam_id, status_id, created_at DESC)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_user_dam_created ON anomalies(user_id, dam_id, created_at DESC)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_danger_status ON anomalies(danger_level_id, status_id)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_origin_status ON anomalies(origin, status_id)",
            "CREATE INDEX IF NOT EXISTS idx_user_client_user_id ON user_client(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_user_client_client_id ON user_client(client_id)",
            "CREATE INDEX IF NOT EXISTS idx_user_client_composite ON user_client(user_id, client_id)",
            "CREATE INDEX IF NOT EXISTS idx_dam_perm_active_users ON dam_permissions(client_id, has_access) WHERE has_access = true",
            "CREATE INDEX IF NOT EXISTS idx_dam_perm_user_active ON dam_permissions(user_id, has_access, client_id) WHERE has_access = true",
            "CREATE INDEX IF NOT EXISTS idx_dam_geo_client_status ON dam(client_id, status_id, latitude, longitude)",
            "CREATE INDEX IF NOT EXISTS idx_instrument_dam_type_active ON instrument(dam_id, instrument_type_id, active)",
            "CREATE INDEX IF NOT EXISTS idx_instrument_type_dam_coords ON instrument(instrument_type_id, dam_id, latitude, longitude)",
            "CREATE INDEX IF NOT EXISTS idx_reading_recent_by_instrument ON reading(instrument_id, date DESC, hour DESC) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_alert_status ON reading(limit_status, instrument_id) WHERE limit_status != 'NORMAL'",
            "CREATE INDEX IF NOT EXISTS idx_instrument_geo_search ON instrument(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL",
            "CREATE INDEX IF NOT EXISTS idx_reading_recent_alerts ON reading(limit_status, date DESC, hour DESC) WHERE limit_status IN ('ATTENTION', 'ALERT', 'EMERGENCY')",
            "CREATE INDEX IF NOT EXISTS idx_reading_active_recent ON reading(active, date DESC, hour DESC) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_instrument_date_value ON reading(instrument_id, date, calculated_value) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_output_date_value ON reading(output_id, date, calculated_value) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_daily_summary ON reading(instrument_id, date) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_user_recent ON reading(user_id, date DESC, hour DESC) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_user_instrument_date ON reading(user_id, instrument_id, date DESC)",
            "CREATE INDEX IF NOT EXISTS idx_reading_complex_filter ON reading(instrument_id, output_id, active, limit_status, date DESC)",
            "CREATE INDEX IF NOT EXISTS idx_reading_trend_analysis ON reading(instrument_id, output_id, date, calculated_value) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_output_active_with_readings ON output(id, active) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_instrument_with_recent_readings ON instrument(id, dam_id, active) WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_timeseries ON reading(instrument_id, output_id, date, hour, calculated_value) WHERE active = true",
            "CREATE INDEX idx_reading_input_value_mapping_both ON reading_input_value_mapping(reading_id, input_value_id)",
            "CREATE INDEX idx_reading_instrument_active_output ON reading(instrument_id, active, output_id)",
            "CREATE INDEX idx_reading_date_hour_instrument_active ON reading(date DESC, hour DESC, instrument_id, active)",
            "CREATE INDEX IF NOT EXISTS idx_reading_composite_main "
            + "ON reading(instrument_id, active, date DESC, hour DESC) "
            + "INCLUDE (calculated_value, limit_status, output_id)",
            "CREATE INDEX IF NOT EXISTS idx_reading_client_aggregation "
            + "ON reading(instrument_id, date DESC, hour DESC) "
            + "WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_rivm_mapping_covering "
            + "ON reading_input_value_mapping(reading_id, input_value_id)",
            "CREATE INDEX IF NOT EXISTS idx_reading_multifilter "
            + "ON reading(instrument_id, output_id, active, limit_status, date DESC) "
            + "INCLUDE (hour, calculated_value)",
            "CREATE INDEX IF NOT EXISTS idx_reading_distinct_date_hour "
            + "ON reading(instrument_id, date DESC, hour DESC, active) "
            + "WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_instrument_dam_active "
            + "ON instrument(dam_id, active, id) "
            + "WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_count_fast "
            + "ON reading(instrument_id, active) "
            + "WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_reading_output_lookup "
            + "ON reading(output_id, active, date DESC, hour DESC) "
            + "WHERE active = true",
            "CREATE INDEX IF NOT EXISTS idx_dam_client_instruments "
            + "ON dam(client_id, id) "
            + "INCLUDE (name)",
            "CREATE INDEX IF NOT EXISTS idx_reading_alerts_only "
            + "ON reading(instrument_id, limit_status, date DESC, hour DESC) "
            + "WHERE limit_status IN ('ATENCAO', 'ALERTA', 'EMERGENCIA', 'INFERIOR', 'SUPERIOR')",
            "CREATE INDEX IF NOT EXISTS idx_reading_batch_lookup "
            + "ON reading(instrument_id, date, hour, active) "
            + "INCLUDE (id, calculated_value, limit_status, output_id)"
        };

        int successCount = 0;
        for (String sql : indexCommands) {
            try {
                jdbcTemplate.execute(sql);
                successCount++;
            } catch (Exception e) {
                log.warn("Erro ao criar índice: {}. Erro: {}", sql, e.getMessage());
            }
        }

        log.info("Criação de índices concluída. {}/{} índices criados com sucesso.",
                successCount, indexCommands.length);

        try {
            log.info("Executando ANALYZE nas tabelas principais...");
            jdbcTemplate.execute("ANALYZE reading");
            jdbcTemplate.execute("ANALYZE reading_input_value_mapping");
            jdbcTemplate.execute("ANALYZE instrument");
            jdbcTemplate.execute("ANALYZE dam");
            log.info("ANALYZE concluído com sucesso.");
        } catch (Exception e) {
            log.warn("Erro ao executar ANALYZE: {}", e.getMessage());
        }
    }
}
