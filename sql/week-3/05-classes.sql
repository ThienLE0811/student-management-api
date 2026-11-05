
-- 1
CREATE TABLE classes (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name  VARCHAR(100) NOT NULL
);

-- 2
ALTER TABLE students ADD COLUMN class_id BIGINT;
-- 3
ALTER TABLE students
    ADD CONSTRAINT fk_students_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT;

INSERT INTO classes (name) VALUES ('Lớp CNTT1'),  ('Lớp CNTT2');


UPDATE students
SET class_id = 1
WHERE id IN (8,9,10);

DELETE FROM classes WHERE id = 1;

-- 4

-- 5


-- 6

-- 7


-- ===== Ghi chú / nhật ký =====
-- FOREIGN KEY (students.class_id -> classes.id): ràng buộc toàn vẹn tham chiếu,
-- đảm bảo class_id luôn trỏ tới 1 dòng có thật trong classes (hoặc NULL nếu cho phép).
-- Quan hệ 1-n: 1 lớp (classes) có nhiều sinh viên (students), 1 sinh viên chỉ thuộc 1 lớp.
--
-- Da thu ca 3 kieu ON DELETE khi xoa 1 lop con sinh vien:
-- - RESTRICT: bi chan, Postgres bao loi 23001
--   "update or delete on table classes violates RESTRICT setting of foreign key
--   constraint fk_students_class on table students" -> khong xoa duoc, phai go
--   sinh vien khoi lop (hoac xoa sinh vien) truoc.
-- - CASCADE: lop bi xoa -> cac sinh vien co class_id = lop do bi xoa theo luon.
-- - SET NULL: lop bi xoa -> sinh vien van con, nhung class_id tu dong chuyen ve NULL
--   (bat buoc cot class_id phai cho phep NULL, khong duoc NOT NULL).
--
-- Loi da gap: sau khi DROP CONSTRAINT (kieu RESTRICT) de doi sang CASCADE, quen
-- ADD CONSTRAINT lai truoc khi DELETE -> lenh DELETE chay thanh cong nhung khong
-- con FK nao rang buoc, class_id cua sinh vien tro toi 1 id khong con ton tai
-- (du lieu "mo coi"). Bai hoc: moi lan doi kieu ON DELETE phai DROP roi ADD lai
-- CONSTRAINT ngay, khong de khoang trong giua 2 buoc.
--
-- Muon doi ON DELETE cua 1 constraint co san thi phai DROP CONSTRAINT roi ADD lai
-- voi ON DELETE moi, vi Postgres khong co lenh sua truc tiep ON DELETE cua FK dang ton tai.

