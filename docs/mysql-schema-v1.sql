CREATE DATABASE IF NOT EXISTS doctor
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE doctor;

CREATE TABLE IF NOT EXISTS admin_user (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'HOSPITAL_ADMIN',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_admin_user_hospital_username (hospital_id, username),
    KEY idx_admin_user_hospital_role (hospital_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hospital (
    id VARCHAR(36) NOT NULL,
    hospital_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    address VARCHAR(255),
    phone VARCHAR(64),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hospital_code (hospital_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS department (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    department_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    category VARCHAR(120),
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_department_code (hospital_id, department_code),
    KEY idx_department_hospital (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clinic_room (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    clinic_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    location VARCHAR(255),
    description VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_clinic_hospital (hospital_id),
    KEY idx_clinic_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS doctor (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    clinic_room_id VARCHAR(64),
    doctor_code VARCHAR(64) NOT NULL,
    name VARCHAR(120) NOT NULL,
    title VARCHAR(64),
    specialty VARCHAR(120),
    introduction TEXT,
    avatar_url VARCHAR(255),
    hot_expert BIT NOT NULL DEFAULT 0,
    consultation_fee INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_doctor_hospital (hospital_id),
    KEY idx_doctor_department (department_id),
    KEY idx_doctor_clinic (clinic_room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS schedule_slot (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    clinic_room_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    slot_date DATE NOT NULL,
    period VARCHAR(32) NOT NULL,
    stock_total INT NOT NULL,
    stock_available INT NOT NULL,
    hot_slot BIT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_slot_hospital (hospital_id),
    KEY idx_slot_doctor_date (doctor_id, slot_date),
    KEY idx_slot_department_date (department_id, slot_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS registration_rule (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    rule_text TEXT NOT NULL,
    notice VARCHAR(255),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_rule_hospital (hospital_id),
    KEY idx_rule_department (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_name VARCHAR(128) NOT NULL,
    chunk_key VARCHAR(64) NOT NULL,
    chunk_text TEXT NOT NULL,
    metadata_json TEXT,
    external_vector_id VARCHAR(128),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chunk_hospital (hospital_id),
    KEY idx_chunk_source (source_type, source_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_document (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    title VARCHAR(180) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_name VARCHAR(180) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    raw_text LONGTEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_doc_hospital (hospital_id),
    KEY idx_doc_source (source_type, source_name),
    KEY idx_doc_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS registration_draft (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    symptom_summary TEXT NOT NULL,
    department_id VARCHAR(64),
    clinic_room_id VARCHAR(64),
    doctor_id VARCHAR(64),
    slot_id VARCHAR(64),
    visit_date DATE,
    visit_period VARCHAR(32),
    status VARCHAR(32) NOT NULL,
    patient_name VARCHAR(64),
    patient_phone VARCHAR(32),
    id_card VARCHAR(32),
    gender VARCHAR(16),
    age INT,
    patient_id VARCHAR(64),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_draft_hospital_session (hospital_id, session_id),
    KEY idx_draft_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS registration_order (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    draft_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    department_id VARCHAR(64) NOT NULL,
    clinic_room_id VARCHAR(64) NOT NULL,
    doctor_id VARCHAR(64) NOT NULL,
    slot_id VARCHAR(64) NOT NULL,
    visit_date DATE NOT NULL,
    visit_period VARCHAR(32) NOT NULL,
    patient_name VARCHAR(64) NOT NULL,
    patient_phone VARCHAR(32) NOT NULL,
    id_card VARCHAR(32) NOT NULL,
    gender VARCHAR(16) NOT NULL,
    age INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    symptom_summary TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY idx_order_order_no (order_no),
    UNIQUE KEY uk_order_draft_id (draft_id),
    KEY idx_order_hospital (hospital_id),
    KEY idx_order_slot (slot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS import_job (
    id VARCHAR(36) NOT NULL,
    hospital_id VARCHAR(64) NOT NULL,
    file_name VARCHAR(128) NOT NULL,
    file_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    storage_path VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    summary_json TEXT,
    error_message TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_import_job_hospital (hospital_id),
    KEY idx_import_job_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
