-- liquibase formatted sql
--changeset system:002-seed-categories context:data

-- ============================================================================
-- 002: наполнение глобальных категорий (расходы + доходы)
-- Источник значений: ExpenseCategory (без NONE) и IncomeSource
-- ============================================================================

-- EXPENSE categories
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Продукты питания', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Транспорт', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Коммунальные услуги', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Здоровье', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Рестораны и кафе', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Одежда и обувь', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Развлечения', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Путешествия', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Образование', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Связь', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Подарки/Праздники', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Налоги', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Домашние животные', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Спорт', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Красота', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Страховки', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Инвестиции', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Дом и быт', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Прочее', NULL, 'EXPENSE', FALSE, 0) ON CONFLICT DO NOTHING;
-- INCOME categories
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Зарплата', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Бизнес', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Подарок', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Инвестиции', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Аренда', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Фриланс', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Дивиденды', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Проценты', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Возврат', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Продажа активов', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Займ', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Бонус', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Кэшбэк', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth) VALUES ('Прочее', NULL, 'INCOME', FALSE, 0) ON CONFLICT DO NOTHING;
--rollback DELETE FROM categories WHERE owner_id IS NULL;
