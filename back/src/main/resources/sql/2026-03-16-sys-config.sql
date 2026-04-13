CREATE TABLE IF NOT EXISTS `sys_config` (
  `key` VARCHAR(128) NOT NULL COMMENT '配置键',
  `value` TEXT NULL COMMENT '配置值，密钥类型存加密信封',
  `type` VARCHAR(32) NOT NULL COMMENT 'STRING/INT/DOUBLE/BOOL/JSON/SECRET',
  `updated_at` DATETIME NULL COMMENT '更新时间',
  `updated_by` BIGINT NULL COMMENT '更新人ID',
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

ALTER TABLE `plagiarism_job`
  ADD COLUMN IF NOT EXISTS `config_snapshot` TEXT NULL COMMENT '任务创建时的配置快照';

INSERT INTO `sys_config` (`key`, `value`, `type`, `updated_at`, `updated_by`) VALUES
('plagiarism.threshold', '0.8', 'DOUBLE', NOW(), NULL),
('plagiarism.topK', '20', 'INT', NOW(), NULL),
('upload.max_files', '20', 'INT', NOW(), NULL),
('upload.max_file_size_mb', '5', 'INT', NOW(), NULL),
('upload.allowed_exts', '[\".java\"]', 'JSON', NOW(), NULL),
('storage.base_path', 'uploads', 'STRING', NOW(), NULL),
('ai.enabled', 'false', 'BOOL', NOW(), NULL),
('ai.base_url', '', 'STRING', NOW(), NULL),
('ai.model', 'qwen-plus', 'STRING', NOW(), NULL),
('ai.timeout_ms', '8000', 'INT', NOW(), NULL),
('ai.api_key', '', 'SECRET', NOW(), NULL)
ON DUPLICATE KEY UPDATE
`value` = VALUES(`value`),
`type` = VALUES(`type`),
`updated_at` = NOW();

