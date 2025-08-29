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
            // Junção entre Checklist e TemplateQuestionnaire
            "CREATE INDEX IF NOT EXISTS idx_checklist_template_checklist_id ON checklist_template_questionnaire(checklist_id)",
            "CREATE INDEX IF NOT EXISTS idx_checklist_template_questionnaire_id ON checklist_template_questionnaire(template_questionnaire_id)",
            // Junção entre Checklist e Dam
            "CREATE INDEX IF NOT EXISTS idx_checklist_dam_checklist_id ON checklist_dam(checklist_id)",
            "CREATE INDEX IF NOT EXISTS idx_checklist_dam_dam_id ON checklist_dam(dam_id)",
            // Junção entre Question e Option
            "CREATE INDEX IF NOT EXISTS idx_question_option_question_id ON question_option(question_id)",
            "CREATE INDEX IF NOT EXISTS idx_question_option_option_id ON question_option(option_id)",
            // Junção entre Answer e Option
            "CREATE INDEX IF NOT EXISTS idx_answer_options_answer_id ON answer_options(answer_id)",
            "CREATE INDEX IF NOT EXISTS idx_answer_options_option_id ON answer_options(option_id)",
            // Índices para checklist_template_questionnaire
            "CREATE INDEX IF NOT EXISTS idx_ck_template_checklist_id ON checklist_template_questionnaire(checklist_id)",
            "CREATE INDEX IF NOT EXISTS idx_ck_template_template_id ON checklist_template_questionnaire(template_questionnaire_id)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_photo_path ON anomaly_photos(image_path)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_geo_bounds ON anomalies(latitude, longitude) WHERE latitude IS NOT NULL AND longitude IS NOT NULL",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_dam_status_created ON anomalies(dam_id, status_id, created_at DESC)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_user_dam_created ON anomalies(user_id, dam_id, created_at DESC)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_danger_status ON anomalies(danger_level_id, status_id)",
            "CREATE INDEX IF NOT EXISTS idx_anomaly_origin_status ON anomalies(origin, status_id)",
            // Novos índices para tabela de junção user_client
            "CREATE INDEX IF NOT EXISTS idx_user_client_user_id ON user_client(user_id)",
            "CREATE INDEX IF NOT EXISTS idx_user_client_client_id ON user_client(client_id)",
            "CREATE INDEX IF NOT EXISTS idx_user_client_composite ON user_client(user_id, client_id)",
            // Índices para consultas de permissão
            "CREATE INDEX IF NOT EXISTS idx_dam_perm_active_users ON dam_permissions(client_id, has_access) WHERE has_access = true",
            "CREATE INDEX IF NOT EXISTS idx_dam_perm_user_active ON dam_permissions(user_id, has_access, client_id) WHERE has_access = true",
            // Índices para consultas geográficas otimizadas
            "CREATE INDEX IF NOT EXISTS idx_dam_geo_client_status ON dam(client_id, status_id, latitude, longitude)"
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
    }
}
