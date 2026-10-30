use lottery_winner_db;

CREATE TABLE winners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    winner_id VARCHAR(50) NOT NULL UNIQUE,

    draw_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    lottery_id BIGINT NOT NULL,
    customer_id BIGINT,
    prize_tier_id BIGINT,

    prize_amount DECIMAL(15,2) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'DECLARED',

    declared_at DATETIME NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    index idx_winner_draw (draw_id),
    index idx_winner_ticket (ticket_id),
    index idx_winner_customer (customer_id),
    index idx_winner_status (status)
    
);

CREATE TABLE prize_claims (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    winner_id BIGINT NOT NULL UNIQUE,
    claim_reference VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    claimed_at DATETIME,
    verified_at DATETIME,
    verified_by BIGINT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_claim_winner
        FOREIGN KEY (winner_id)
        REFERENCES winners(id),

    index idx_claim_status(status)
);