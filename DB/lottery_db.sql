use lottery_db;

CREATE TABLE lotteries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lottery_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    ticket_price DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    sales_start_at DATETIME NOT NULL,
    sales_end_at DATETIME NOT NULL,
    draw_at DATETIME NOT NULL,
    max_tickets BIGINT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_ticket_price
        CHECK (ticket_price > 0),

    CONSTRAINT chk_lottery_dates
        CHECK (
            sales_start_at < sales_end_at
            AND sales_end_at <= draw_at
        ),

    index idx_lottery_status (status),
    index idx_lottery_draw_at (draw_at)
    
);

CREATE TABLE prize_tiers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    lottery_id BIGINT NOT NULL,

    tier_number INT NOT NULL,
    tier_name VARCHAR(100) NOT NULL,

    prize_amount DECIMAL(15,2) NOT NULL,
    winner_count INT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_lottery_tier
        UNIQUE (lottery_id, tier_number),

    CONSTRAINT chk_prize_amount
        CHECK (prize_amount >= 0),

    CONSTRAINT chk_winner_count
        CHECK (winner_count > 0),

    CONSTRAINT fk_prize_lottery
        FOREIGN KEY (lottery_id)
        REFERENCES lotteries(id)
);

CREATE TABLE lottery_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    lottery_id BIGINT NOT NULL UNIQUE,

    number_count INT NOT NULL,
    min_number INT NOT NULL,
    max_number INT NOT NULL,

    allow_multiple_winners BOOLEAN NOT NULL DEFAULT TRUE,

    allow_ticket_cancellation BOOLEAN NOT NULL DEFAULT FALSE,

    cancellation_cutoff_minutes INT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rules_lottery
        FOREIGN KEY (lottery_id)
        REFERENCES lotteries(id)
);
