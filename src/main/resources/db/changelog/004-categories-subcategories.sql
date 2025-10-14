-- liquibase formatted sql
--changeset system:004-categories-subcategories context:data

-- 004: Добавление подкатегорий для существующих глобальных EXPENSE категорий
-- Продукты питания -> Супермаркет, Еда
-- Коммунальные услуги -> Электричество, Вода, Интернет, Квартплата
-- Рестораны и кафе -> Доставка еды

-- Продукты питания
INSERT INTO categories (name, owner_id, type, archived, depth, parent_id)
SELECT 'Супермаркет', NULL, 'EXPENSE', FALSE, 1, c.id FROM categories c
 WHERE c.name='Продукты питания' AND c.owner_id IS NULL AND c.type='EXPENSE'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth, parent_id)
SELECT 'Еда', NULL, 'EXPENSE', FALSE, 1, c.id FROM categories c
 WHERE c.name='Продукты питания' AND c.owner_id IS NULL AND c.type='EXPENSE'
ON CONFLICT DO NOTHING;

-- Коммунальные услуги
INSERT INTO categories (name, owner_id, type, archived, depth, parent_id)
SELECT 'Электричество', NULL, 'EXPENSE', FALSE, 1, c.id FROM categories c
 WHERE c.name='Коммунальные услуги' AND c.owner_id IS NULL AND c.type='EXPENSE'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth, parent_id)
SELECT 'Вода', NULL, 'EXPENSE', FALSE, 1, c.id FROM categories c
 WHERE c.name='Коммунальные услуги' AND c.owner_id IS NULL AND c.type='EXPENSE'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth, parent_id)
SELECT 'Интернет', NULL, 'EXPENSE', FALSE, 1, c.id FROM categories c
 WHERE c.name='Коммунальные услуги' AND c.owner_id IS NULL AND c.type='EXPENSE'
ON CONFLICT DO NOTHING;
INSERT INTO categories (name, owner_id, type, archived, depth, parent_id)
SELECT 'Квартплата', NULL, 'EXPENSE', FALSE, 1, c.id FROM categories c
 WHERE c.name='Коммунальные услуги' AND c.owner_id IS NULL AND c.type='EXPENSE'
ON CONFLICT DO NOTHING;

-- Рестораны и кафе
INSERT INTO categories (name, owner_id, type, archived, depth, parent_id)
SELECT 'Доставка еды', NULL, 'EXPENSE', FALSE, 1, c.id FROM categories c
 WHERE c.name='Рестораны и кафе' AND c.owner_id IS NULL AND c.type='EXPENSE'
ON CONFLICT DO NOTHING;

--rollback DELETE FROM categories WHERE owner_id IS NULL AND type='EXPENSE' AND name IN ('Супермаркет','Еда','Электричество','Вода','Интернет','Квартплата','Доставка еды');

