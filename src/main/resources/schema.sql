-- =============================================================================
-- Prescription Management System - Standalone Database Schema DDL
-- Compatible with MySQL 8.x+ / MariaDB / Relational RDBMS
-- =============================================================================

CREATE DATABASE IF NOT EXISTS PrescriptionManagement;
USE PrescriptionManagement;

-- -----------------------------------------------------------------------------
-- 1. Users Table (Doctors, Admins)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    registration_number VARCHAR(80) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    password VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT uk_users_registration_number UNIQUE (registration_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 2. Clients Table (Patients)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    date_of_birth DATE NULL,
    phone_number VARCHAR(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3. Records Table (Prescription Headers)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(120) NULL,
    user_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    record_date DATE NOT NULL,
    diagnosis VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    CONSTRAINT fk_records_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_records_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uk_records_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_records_user_id (user_id),
    INDEX idx_records_client_id (client_id),
    INDEX idx_records_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4. Record Entries Table (Prescription Medications / Items)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS record_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    entry_name VARCHAR(200) NOT NULL,
    quantity INT NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration INT NOT NULL,
    instructions VARCHAR(1000) NULL,
    CONSTRAINT fk_entries_record FOREIGN KEY (record_id) REFERENCES records(id) ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_entries_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- Initial Seed Data: Default Admin Account
-- Password: 'Admin@12345' (BCrypt Encrypted)
-- -----------------------------------------------------------------------------
INSERT INTO users (name, registration_number, phone_number, password, role)
SELECT 'System Administrator', 'ADMIN001', '+1234567890', 
       '$2a$10$tZzC2P9p/kF28r3jGvjC7ehw0b6BqXk6v4d0rD0sE0kU9g5m7Q1k6', 'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE registration_number = 'ADMIN001'
);
