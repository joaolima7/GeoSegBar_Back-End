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
            "CREATE INDEX IF NOT EXISTS idx_ck_template_template_id ON checklist_template_questionnaire(template_questionnaire_id)",};

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
