-- ============================================================
-- VEHICLE IDENTIFICATION SYSTEM (MySQL 8.0) - COMPLETE FIXED
-- ============================================================

DROP DATABASE IF EXISTS vis_database;
CREATE DATABASE vis_database;
USE vis_database;

SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- USERS (added is_active column)
-- ============================================================
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    is_active TINYINT(1) DEFAULT 1
);

-- ============================================================
-- CUSTOMERS
-- ============================================================
CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    address VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(100),
    id_number VARCHAR(20)
);

-- ============================================================
-- VEHICLES
-- ============================================================
CREATE TABLE vehicles (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    registration_number VARCHAR(20) UNIQUE,
    make VARCHAR(50),
    model VARCHAR(50),
    year INT,
    color VARCHAR(30),
    engine_number VARCHAR(50),
    chassis_number VARCHAR(50),
    owner_id INT,
    registration_date DATE,
    is_stolen TINYINT(1) DEFAULT 0,
    FOREIGN KEY (owner_id) REFERENCES customers(customer_id)
);

-- ============================================================
-- SERVICE RECORDS
-- ============================================================
CREATE TABLE service_records (
    service_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT,
    service_date DATE,
    service_type VARCHAR(50),
    description TEXT,
    cost DECIMAL(10,2),
    workshop_name VARCHAR(100),
    mileage INT,
    next_service_date DATE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

-- ============================================================
-- CUSTOMER QUERIES
-- ============================================================
CREATE TABLE customer_queries (
    query_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT,
    vehicle_id INT,
    query_date DATE,
    query_text TEXT,
    response_text TEXT,
    status VARCHAR(30),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

-- ============================================================
-- INSURANCE
-- ============================================================
CREATE TABLE insurance_policies (
    policy_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT,
    customer_id INT,
    policy_number VARCHAR(50),
    provider VARCHAR(50),
    coverage_type VARCHAR(50),
    start_date DATE,
    end_date DATE,
    premium DECIMAL(10,2),
    is_active TINYINT(1) DEFAULT 1,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- ============================================================
-- POLICE REPORTS
-- ============================================================
CREATE TABLE police_reports (
    report_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT,
    report_date DATE,
    report_type VARCHAR(50),
    description TEXT,
    officer_name VARCHAR(100),
    station VARCHAR(100),
    case_number VARCHAR(50),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

-- ============================================================
-- VIOLATIONS
-- ============================================================
CREATE TABLE violations (
    violation_id INT AUTO_INCREMENT PRIMARY KEY,
    vehicle_id INT,
    violation_date DATE,
    violation_type VARCHAR(50),
    fine_amount DECIMAL(10,2),
    status VARCHAR(30),
    location VARCHAR(100) DEFAULT 'UNKNOWN',
    officer_name VARCHAR(100),
    paid_date DATE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id)
);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- VIEWS (required by controllers)
-- ============================================================

-- VIEW: vehicle_owner_details (used by VehiclesController)
CREATE OR REPLACE VIEW vehicle_owner_details AS
SELECT
    v.vehicle_id,
    v.registration_number,
    v.make,
    v.model,
    v.year,
    v.color,
    v.engine_number,
    v.chassis_number,
    v.owner_id,
    v.registration_date,
    v.is_stolen,
    c.name AS owner_name,
    c.phone AS owner_phone,
    c.email AS owner_email
FROM vehicles v
LEFT JOIN customers c ON v.owner_id = c.customer_id;

-- VIEW: police_report_overview (used by PoliceController)
CREATE OR REPLACE VIEW police_report_overview AS
SELECT
    pr.report_id,
    pr.vehicle_id,
    v.registration_number,
    CONCAT(v.make, ' ', v.model) AS vehicle_description,
    pr.report_date,
    pr.report_type,
    pr.description,
    pr.officer_name,
    pr.station,
    pr.case_number
FROM police_reports pr
LEFT JOIN vehicles v ON pr.vehicle_id = v.vehicle_id;

-- VIEW: customer_query_overview (used by QueriesController)
CREATE OR REPLACE VIEW customer_query_overview AS
SELECT
    cq.query_id,
    cq.customer_id,
    c.name AS customer_name,
    cq.vehicle_id,
    v.registration_number,
    cq.query_date,
    cq.query_text,
    cq.response_text,
    cq.status
FROM customer_queries cq
LEFT JOIN customers c ON cq.customer_id = c.customer_id
LEFT JOIN vehicles v ON cq.vehicle_id = v.vehicle_id;

-- VIEW: vehicle_service_summary (used by ServicesController)
CREATE OR REPLACE VIEW vehicle_service_summary AS
SELECT
    sr.service_id,
    sr.vehicle_id,
    v.registration_number,
    CONCAT(v.make, ' ', v.model) AS vehicle_description,
    sr.service_date,
    sr.service_type,
    sr.description,
    sr.cost,
    sr.workshop_name,
    sr.mileage,
    sr.next_service_date
FROM service_records sr
LEFT JOIN vehicles v ON sr.vehicle_id = v.vehicle_id;

-- ============================================================
-- STORED PROCEDURES
-- ============================================================
DELIMITER $$

-- LOGIN PROCEDURE
CREATE PROCEDURE authenticate_user(
    IN p_username VARCHAR(50),
    IN p_password VARCHAR(255)
)
BEGIN
    SELECT
        user_id,
        username,
        role,
        full_name,
        email,
        phone
    FROM users
    WHERE username = p_username
      AND password_hash = p_password
      AND is_active = 1
    LIMIT 1;
END $$

-- ADD VEHICLE
CREATE PROCEDURE add_vehicle(
    IN p_reg_number VARCHAR(20),
    IN p_make VARCHAR(50),
    IN p_model VARCHAR(50),
    IN p_year INT,
    IN p_owner_id INT,
    IN p_color VARCHAR(30),
    IN p_engine_number VARCHAR(50),
    IN p_chassis_number VARCHAR(50)
)
BEGIN
    INSERT INTO vehicles (registration_number, make, model, year, owner_id, color, engine_number, chassis_number, registration_date)
    VALUES (p_reg_number, p_make, p_model, p_year, p_owner_id, p_color, p_engine_number, p_chassis_number, CURDATE());
    SELECT LAST_INSERT_ID() AS vehicle_id;
END $$

-- ADD POLICE REPORT
CREATE PROCEDURE add_police_report(
    IN p_vehicle_id INT,
    IN p_report_date DATE,
    IN p_report_type VARCHAR(50),
    IN p_description TEXT,
    IN p_officer_name VARCHAR(100),
    IN p_station VARCHAR(100),
    IN p_case_number VARCHAR(50)
)
BEGIN
    INSERT INTO police_reports (vehicle_id, report_date, report_type, description, officer_name, station, case_number)
    VALUES (p_vehicle_id, p_report_date, p_report_type, p_description, p_officer_name, p_station, p_case_number);
    SELECT LAST_INSERT_ID() AS report_id;
END $$

-- ADD SERVICE RECORD
CREATE PROCEDURE add_service_record(
    IN p_vehicle_id INT,
    IN p_service_date DATE,
    IN p_service_type VARCHAR(50),
    IN p_description TEXT,
    IN p_cost DECIMAL(10,2),
    IN p_workshop_name VARCHAR(100),
    IN p_mileage INT,
    IN p_next_service_date DATE
)
BEGIN
    INSERT INTO service_records (vehicle_id, service_date, service_type, description, cost, workshop_name, mileage, next_service_date)
    VALUES (p_vehicle_id, p_service_date, p_service_type, p_description, p_cost, p_workshop_name, p_mileage, p_next_service_date);
    SELECT LAST_INSERT_ID() AS service_id;
END $$

-- ADD INSURANCE POLICY
CREATE PROCEDURE add_insurance_policy(
    IN p_vehicle_id INT,
    IN p_customer_id INT,
    IN p_policy_number VARCHAR(50),
    IN p_provider VARCHAR(50),
    IN p_coverage_type VARCHAR(50),
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_premium DECIMAL(10,2)
)
BEGIN
    INSERT INTO insurance_policies (vehicle_id, customer_id, policy_number, provider, coverage_type, start_date, end_date, premium, is_active)
    VALUES (p_vehicle_id, p_customer_id, p_policy_number, p_provider, p_coverage_type, p_start_date, p_end_date, p_premium, 1);
    SELECT LAST_INSERT_ID() AS policy_id;
END $$

-- ADD CUSTOMER QUERY
CREATE PROCEDURE add_customer_query(
    IN p_customer_id INT,
    IN p_vehicle_id INT,
    IN p_query_date DATE,
    IN p_query_text TEXT,
    IN p_status VARCHAR(30)
)
BEGIN
    INSERT INTO customer_queries (customer_id, vehicle_id, query_date, query_text, status)
    VALUES (p_customer_id, p_vehicle_id, p_query_date, p_query_text, p_status);
    SELECT LAST_INSERT_ID() AS query_id;
END $$

-- REGISTER USER
CREATE PROCEDURE register_user(
    IN p_username VARCHAR(50),
    IN p_password VARCHAR(255),
    IN p_role VARCHAR(30),
    IN p_full_name VARCHAR(100),
    IN p_email VARCHAR(100),
    IN p_phone VARCHAR(20)
)
BEGIN
    INSERT INTO users (username, password_hash, role, full_name, email, phone, is_active)
    VALUES (p_username, p_password, p_role, p_full_name, p_email, p_phone, 1);
    SELECT LAST_INSERT_ID() AS user_id;
END $$

-- GET VEHICLE HISTORY
CREATE PROCEDURE get_vehicle_history(
    IN p_reg_number VARCHAR(20)
)
BEGIN
    SELECT v.*, c.name AS owner_name
    FROM vehicles v
    LEFT JOIN customers c ON v.owner_id = c.customer_id
    WHERE v.registration_number LIKE CONCAT('%', p_reg_number, '%');
END $$

DELIMITER ;

-- ============================================================
-- SEED DATA
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;

-- USERS (with is_active)
INSERT INTO users (username, password_hash, role, full_name, email, phone, is_active) VALUES
('admin',     'admin123', 'Admin',     'System Administrator',   'admin@vis.co.za',        '0111001001', 1),
('officer1',  'pass123',  'Police',    'Sgt. Thabo Mokoena',     'thabo.m@saps.gov.za',    '0123456789', 1),
('officer2',  'pass123',  'Police',    'Det. Lerato Dlamini',    'lerato.d@saps.gov.za',   '0123456790', 1),
('mechanic1', 'pass123',  'Workshop',  'Johan van der Merwe',    'johan@autofix.co.za',    '0215551234', 1),
('agent1',    'pass123',  'Insurance', 'Amanda Botha',           'amanda@sanlam.co.za',    '0117894561', 1);

-- CUSTOMERS
INSERT INTO customers (name, address, phone, email, id_number) VALUES
('Kagiso Molefe',   'Sandton',  '0821234567', 'kagiso@email.co.za', '9001015800089'),
('Lerato Mahlangu', 'PMB',      '0832345678', 'lerato@email.co.za', '9202035800091'),
('Johan Botha',     'Pretoria', '0843456789', 'johan@email.co.za',  '8805015800085'),
('Nomsa Dlamini',   'Durban',   '0854567890', 'nomsa@email.co.za',  '9507035800093');

-- VEHICLES
INSERT INTO vehicles (registration_number, make, model, year, color, engine_number, chassis_number, owner_id, registration_date, is_stolen) VALUES
('GP123456',  'Toyota', 'Hilux',    2020, 'White',  'ENG1', 'CH1', 1, '2020-03-15', 0),
('KZN789012', 'VW',     'Polo',     2019, 'Silver', 'ENG2', 'CH2', 2, '2019-07-22', 0),
('GP234567',  'BMW',    '3 Series', 2021, 'Black',  'ENG3', 'CH3', 3, '2021-01-10', 0),
('KZN890123', 'Ford',   'Ranger',   2018, 'Blue',   'ENG4', 'CH4', 4, '2018-11-05', 0);

-- SERVICE RECORDS
INSERT INTO service_records (vehicle_id, service_date, service_type, description, cost, workshop_name, mileage, next_service_date) VALUES
(1, '2023-01-15', 'Oil Change',    'Full oil service completed',  850.00,  'AutoFix',    45000, '2023-07-15'),
(2, '2023-02-10', 'Major Service', 'Full 90,000 km major service', 4500.00, 'QuickServe', 67000, '2024-02-10');

-- POLICE REPORTS
INSERT INTO police_reports (vehicle_id, report_date, report_type, description, officer_name, station, case_number) VALUES
(1, '2023-02-10', 'Accident', 'Minor fender bender on N1',         'Sgt Mokoena', 'Sandton SAPS', 'CASE001'),
(2, '2023-05-15', 'Theft',    'Vehicle stolen from parking lot',    'Det Dlamini',  'Durban SAPS',  'CASE002');

-- VIOLATIONS
INSERT INTO violations (vehicle_id, violation_date, violation_type, fine_amount, status, location, officer_name, paid_date) VALUES
(1, '2023-02-15', 'Speeding',   1500.00, 'Paid',   'N1 Highway', 'Officer A', '2023-02-20'),
(2, '2023-03-10', 'Red Light',  1000.00, 'Unpaid', 'CBD',        'Officer B', NULL);

-- INSURANCE POLICIES
INSERT INTO insurance_policies (vehicle_id, customer_id, policy_number, provider, coverage_type, start_date, end_date, premium, is_active) VALUES
(1, 1, 'POL-2023-001', 'Outsurance',  'Comprehensive',            '2023-01-01', '2024-01-01', 1250.00, 1),
(2, 2, 'POL-2023-002', 'Santam',      'Third Party Fire & Theft', '2023-07-01', '2024-07-01',  750.00, 1),
(3, 3, 'POL-2022-003', 'Discovery',   'Comprehensive',            '2022-01-10', '2023-01-10', 2100.00, 0),
(4, 4, 'POL-2023-004', 'MiWay',       'Third Party',              '2023-11-01', '2024-11-01',  450.00, 1);

-- CUSTOMER QUERIES
INSERT INTO customer_queries (customer_id, vehicle_id, query_date, query_text, response_text, status) VALUES
(1, 1, '2024-01-05', 'When is my next service due?',         'Your next service is due on 2023-07-15 at AutoFix.', 'Resolved'),
(2, 2, '2024-02-10', 'Is my vehicle still under warranty?', NULL,                                                  'Pending'),
(3, 3, '2024-03-01', 'What is the status of my policy?',    'Your policy expired on 2023-01-10. Please renew.',   'Resolved');

SET FOREIGN_KEY_CHECKS = 1;


-- ============================================================
-- TIMEZONE FIX (run this if you get "Location is not set")
-- ============================================================
SET GLOBAL time_zone = '+02:00';
SET time_zone = '+02:00';
