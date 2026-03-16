-- Assignment-scoped plagiarism detection schema
--
-- Apply (example):
-- mysql -u root -p sch1 < sql/001_assignment_submission_plagiarism_schema.sql

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS assignment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  clazz_id INT NOT NULL,
  title VARCHAR(200) NOT NULL,
  description TEXT NULL,
  deadline DATETIME NOT NULL,
  allow_resubmit TINYINT NOT NULL DEFAULT 1,
  max_files INT NOT NULL DEFAULT 20,
  create_time DATETIME NOT NULL,
  update_time DATETIME NULL,
  INDEX idx_assignment_clazz_deadline (clazz_id, deadline)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  version INT NOT NULL,
  submit_time DATETIME NOT NULL,
  is_latest TINYINT NOT NULL DEFAULT 1,
  is_valid TINYINT NOT NULL DEFAULT 1,
  parse_ok_files INT NOT NULL DEFAULT 0,
  total_files INT NOT NULL DEFAULT 0,
  INDEX idx_submission_assignment_latest (assignment_id, is_latest),
  INDEX idx_submission_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS submission_file (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  submission_id BIGINT NOT NULL,
  filename VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  sha256 CHAR(64) NOT NULL,
  bytes BIGINT NOT NULL,
  parse_status VARCHAR(30) NOT NULL,
  parse_error VARCHAR(500) NULL,
  INDEX idx_submission_file_submission (submission_id),
  INDEX idx_submission_file_sha256 (sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS plagiarism_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  assignment_id BIGINT NOT NULL,
  status VARCHAR(30) NOT NULL,
  params_json TEXT NULL,
  progress_total INT NOT NULL DEFAULT 0,
  progress_done INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL,
  start_time DATETIME NULL,
  end_time DATETIME NULL,
  error_msg VARCHAR(800) NULL,
  INDEX idx_plagiarism_job_assignment (assignment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS similarity_pair (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT NOT NULL,
  student_a BIGINT NOT NULL,
  student_b BIGINT NOT NULL,
  score INT NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  teacher_note VARCHAR(800) NULL,
  create_time DATETIME NOT NULL,
  INDEX idx_pair_job_score (job_id, score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS similarity_evidence (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pair_id BIGINT NOT NULL,
  type VARCHAR(50) NOT NULL,
  summary VARCHAR(800) NOT NULL,
  weight INT NOT NULL,
  payload_json TEXT NULL,
  INDEX idx_evidence_pair (pair_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ai_explanation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  pair_id BIGINT NOT NULL,
  model VARCHAR(100) NOT NULL,
  status VARCHAR(30) NOT NULL,
  latency_ms INT NULL,
  result TEXT NULL,
  error_msg VARCHAR(800) NULL,
  create_time DATETIME NOT NULL,
  INDEX idx_ai_pair (pair_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;