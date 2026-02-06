-- Таблица для идемпотентности
-- Хранит eventId всех уже обработанных событий
CREATE TABLE IF NOT EXISTS processed_events (
    event_id        UUID PRIMARY KEY,
    client_id       BIGINT NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE processed_events IS 'Events that have already been processed (idempotency)';

-- Таблица обращений клиентов
-- Используется для проверки "активных обращений" по клиенту и категории
CREATE TABLE IF NOT EXISTS appeals (
    id          SERIAL PRIMARY KEY,
    client_id   BIGINT NOT NULL,
    category    VARCHAR(100) NOT NULL,
    theme       VARCHAR(255) NOT NULL,          -- сделала NOT NULL, если тема обязательна
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

COMMENT ON TABLE appeals IS 'Client appeals used for duplicate checks by category';

-- Индекс для быстрого поиска активных обращений по клиенту + категории
CREATE INDEX IF NOT EXISTS idx_appeals_active
    ON appeals (client_id, category, status)
    WHERE status = 'ACTIVE';   -- частичный индекс - ещё лучше для производительности

-- Ограничение на допустимые статусы
ALTER TABLE appeals
    ADD CONSTRAINT chk_appeals_status
        CHECK (status IN ('ACTIVE', 'CLOSED'));

-- Дополнительный индекс по client_id (если часто ищем все обращения клиента)
CREATE INDEX IF NOT EXISTS idx_appeals_client_id
    ON appeals (client_id);