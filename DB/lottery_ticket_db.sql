use lottery_ticket_db;

CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(50) NOT NULL UNIQUE,
    lottery_id BIGINT NOT NULL,
    customer_id BIGINT,
    agent_id BIGINT,
    ticket_number VARCHAR(100) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',

    purchased_at DATETIME NOT NULL,
    issued_at DATETIME,
    cancelled_at DATETIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_lottery_ticket_number
        UNIQUE (lottery_id, ticket_number),

    INDEX idx_ticket_lottery (lottery_id),
    INDEX idx_ticket_customer (customer_id),
    INDEX idx_ticket_agent (agent_id),
    INDEX idx_ticket_status (status)
);

CREATE TABLE ticket_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    transaction_reference VARCHAR(100) NOT NULL UNIQUE,
    transaction_type VARCHAR(30) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(30),
    external_reference VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_ticket
        FOREIGN KEY (ticket_id)
        REFERENCES tickets(id),

    INDEX idx_transaction_ticket (ticket_id)
);