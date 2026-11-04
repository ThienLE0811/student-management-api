-- Buổi 3 — PRIMARY KEY, NOT NULL, UNIQUE, CHECK, DEFAULT, GENERATED ALWAYS AS IDENTITY
-- Tự chạy từng câu INSERT sai trong pgAdmin và CHÉP LẠI NGUYÊN VĂN thông báo lỗi
-- (không tự bịa lỗi — mục tiêu bài tập là đọc hiểu lỗi thật của PostgreSQL).

-- Nội dung cần nhớ:
--   PRIMARY KEY  -> định danh duy nhất mỗi dòng, tự bao gồm NOT NULL + UNIQUE
--   NOT NULL     -> cấm để trống giá trị cột đó khi insert
--   UNIQUE       -> không cho 2 dòng trùng giá trị ở cột đó
--   CHECK (cond) -> ràng buộc điều kiện logic phải đúng cho mọi dòng
--   DEFAULT val  -> không truyền giá trị thì Postgres tự điền val
--   GENERATED ALWAYS AS IDENTITY -> cột tự tăng chuẩn SQL (thay cho SERIAL cũ);
--                                   không được tự gán giá trị cho cột này khi insert

DROP TABLE IF EXISTS students;

CREATE TABLE students (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  birth_date DATE CHECK (birth_date <= CURRENT_DATE),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Vài dòng hợp lệ để có dữ liệu sẵn (không tự gõ id, cột này tự sinh)
INSERT INTO students (full_name, email, birth_date) VALUES
  ('Nguyễn Văn An', 'an.nguyenvan@gmail.com', '2004-03-15'),
  ('Trần Thị Bích', 'bich.tran@gmail.com', '2004-07-22');


-- ===== 5 kiểu INSERT sai — tự chạy từng câu, điền lỗi thật vào comment bên dưới =====

-- 1. Thiếu full_name (vi phạm NOT NULL)
INSERT INTO students (full_name, email, birth_date) VALUES
  (null, 'an.nguyenvan@gmail.com', '2004-03-15');
-- Lỗi:
-- ERROR:  null value in column "full_name" of relation "students" violates not-null constraint
-- SQL state: 23502
-- Detail: Failing row contains (15, null, an.nguyenvan@gmail.com, 2004-03-15, 2026-11-04 15:12:10.725663).

-- 2. Email trùng với dòng đã có (vi phạm UNIQUE)
INSERT INTO students (full_name, email, birth_date) VALUES
  ('Lê La', 'anh.vuthimai@gmail.com', '2004-03-15');
-- Lỗi:
-- ERROR:  duplicate key value violates unique constraint "students_email_key"
-- SQL state: 23505
-- Detail: Key (email)=(anh.vuthimai@gmail.com) already exists.
INSERT INTO students (full_name, email, birth_date) VALUES
    ('Lê La', 'anh.vuthimai@gmail.com', '2004-03-15');

-- 3. Ngày sinh ở tương lai (vi phạm CHECK)
INSERT INTO students (full_name, email, birth_date) VALUES
  ('Lê La', 'an.nguyenvan@gmail.com', '2030-03-15');
-- Lỗi:
-- ERROR:  new row for relation "students" violates check constraint "students_birth_date_check"
-- SQL state: 23514
-- Detail: Failing row contains (18, Lê La, an.nguyenvan@gmail.com, 2030-03-15, 2026-11-04 15:15:43.714866).

-- 4. Tự gán id cho cột GENERATED ALWAYS AS IDENTITY
INSERT INTO students (id, full_name, email, birth_date) VALUES
  (99, 'Lê La', 'an.nguyenvan@gmail.com', '2000-03-15');
-- Lỗi:
-- ERROR:  cannot insert a non-DEFAULT value into column "id"
-- SQL state: 428C9
-- Detail: Column "id" is an identity column defined as GENERATED ALWAYS.
-- Hint: Use OVERRIDING SYSTEM VALUE to override.

-- 5. Sai kiểu dữ liệu ở cột DATE
INSERT INTO students (full_name, email, birth_date) VALUES
  ('Lê La', 'an.nguyenvan@gmail.com', 'khong-phai-ngay');
-- Lỗi:
-- ERROR:  invalid input syntax for type date: "khong-phai-ngay"
-- SQL state: 22007
