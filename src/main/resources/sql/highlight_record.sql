CREATE TABLE IF NOT EXISTS camunda_highlight_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    process_instance_id VARCHAR(64) NOT NULL,
    process_definition_id VARCHAR(64),
    execution_id VARCHAR(64),
    element_id VARCHAR(255) NOT NULL,
    element_name VARCHAR(255),
    element_type VARCHAR(64),
    event_type VARCHAR(32),
    start_time DATETIME,
    end_time DATETIME,
    assignee VARCHAR(64),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_instance_id (process_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
