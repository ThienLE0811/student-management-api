-- Buoi 22 - Flyway V5: them optimistic locking cho classes bang cot
-- version (@Version), de chan lost update khi 2 request cung ghi danh vao
-- 1 lop (da tai hien bug o V4, chua co lock).

ALTER TABLE classes
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE classes
    ALTER COLUMN version DROP DEFAULT;
