CREATE TABLE staff_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0),
    name VARCHAR(127) NOT NULL,
    age TINYINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    timezone VARCHAR(32) NOT NULL,
    role_id BIGINT NOT NULL,
    extra_remarks VARCHAR(1024),
    accepted BOOL NULL,
    handled_by BIGINT NULL,
    handled_at TIMESTAMP(0) NULL
);
