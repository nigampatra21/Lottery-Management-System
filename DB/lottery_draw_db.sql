use lottery_draw_db;
CREATE TABLE draws (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    draw_id VARCHAR(50) NOT NULL UNIQUE,
    lottery_id BIGINT NOT NULL,
    scheduled_at DATETIME NOT NULL,
    started_at DATETIME,
    completed_at DATETIME,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    execution_reference VARCHAR(150),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_draw_lottery (lottery_id),
    INDEX idx_draw_status (status)
);
CREATE TABLE winning_numbers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    draw_id BIGINT NOT NULL,
    sequence_number INT NOT NULL,
    winning_number INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_draw_sequence
        UNIQUE (draw_id, sequence_number),

    CONSTRAINT uq_draw_number
        UNIQUE (draw_id, winning_number),

    CONSTRAINT fk_winning_draw
        FOREIGN KEY (draw_id)
        REFERENCES draws(id)
);