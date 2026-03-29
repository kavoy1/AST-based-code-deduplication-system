package com.ast.back.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SchemaCompatibilityRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaCompatibilityRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaCompatibilityRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureConfigSnapshotColumn();
    }

    private void ensureConfigSnapshotColumn() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'plagiarism_job'
                      AND COLUMN_NAME = 'config_snapshot'
                    """,
                    Integer.class
            );

            if (count != null && count > 0) {
                return;
            }

            jdbcTemplate.execute(
                    """
                    ALTER TABLE plagiarism_job
                    ADD COLUMN config_snapshot TEXT NULL COMMENT '任务创建时的配置快照'
                    """
            );
            log.info("Added missing column plagiarism_job.config_snapshot");
        } catch (Exception ex) {
            log.warn("Failed to ensure plagiarism_job.config_snapshot exists: {}", ex.getMessage());
        }
    }
}
