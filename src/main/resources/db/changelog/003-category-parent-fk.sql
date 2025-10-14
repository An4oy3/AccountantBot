-- liquibase formatted sql
--changeset system:003-category-parent-fk context:ddl

-- 003: Добавление индекса и внешнего ключа для иерархии категорий (parent_id -> categories.id)
CREATE INDEX IF NOT EXISTS idx_category_parent ON categories(parent_id);
ALTER TABLE categories
    ADD CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL;

--rollback ALTER TABLE categories DROP CONSTRAINT IF EXISTS fk_category_parent;
--rollback DROP INDEX IF EXISTS idx_category_parent;

