-- Buoi 7 (vung giao AI): sinh du lieu mau ~50 sinh vien, 5 lop, 6 khoa, cac luot dang ky
-- Sau khi chay: tu kiem tra du lieu co thoa rang buoc (UNIQUE email, UNIQUE(student_id,course_id),
-- CHECK birth_date <= CURRENT_DATE, FK class_id/student_id/course_id).

-- ===== 0) Xoa lai tu dau (dung thu tu phu thuoc) =====
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS classes;

-- ===== 1) CREATE TABLE =====
CREATE TABLE classes (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE courses (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name VARCHAR(100) NOT NULL,
    credit INT
);

CREATE TABLE students (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    birth_date DATE CHECK (birth_date <= CURRENT_DATE),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    class_id BIGINT REFERENCES classes(id) ON DELETE SET NULL
);

CREATE TABLE enrollments (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    score NUMERIC,
    CONSTRAINT uq_enrollments_student_course UNIQUE (student_id, course_id)
);

-- ===== 2) SEED: 5 lop =====
INSERT INTO classes (name)
SELECT 'Lop CNTT' || g FROM generate_series(1, 5) AS g;

-- ===== 3) SEED: 6 khoa hoc =====
INSERT INTO courses (name, credit) VALUES
('Nhap mon lap trinh', 3),
('Cau truc du lieu va giai thuat', 4),
('Co so du lieu', 3),
('Mang may tinh', 3),
('He dieu hanh', 3),
('Ky thuat phan mem', 4);

-- ===== 4) SEED: 50 sinh vien =====
-- g % 10 = 0 -> khong gan lop (de co du lieu test LEFT JOIN "sinh vien chua co lop")
INSERT INTO students (full_name, email, birth_date, class_id)
SELECT
    (ARRAY['Nguyen','Tran','Le','Pham','Hoang','Vu','Dang','Bui','Do','Ngo'])[(g % 10) + 1]
        || ' ' || (ARRAY['Van','Thi'])[(g % 2) + 1]
        || ' ' || (ARRAY['An','Binh','Cuong','Duc','Giang','Nam','Mai','Huy','Linh','Long',
                         'Hoa','Hung','Lan','Nghia','Thu','Tung','Hanh','Khanh','Nga','Phuc'])[(g % 20) + 1] AS full_name,
    'sv' || g || '@gmail.com' AS email,
    (DATE '2002-01-01' + ((g * 37) % 1800)) AS birth_date,
    CASE WHEN g % 10 = 0 THEN NULL ELSE ((g % 5) + 1) END AS class_id
FROM generate_series(1, 50) AS g;

-- ===== 5) SEED: enrollments (moi sinh vien dang ky 2-3 khoa, khong trung UNIQUE) =====
INSERT INTO enrollments (student_id, course_id)
SELECT s.id, ((s.id - 1) % 6) + 1 FROM students s
UNION ALL
SELECT s.id, (((s.id - 1) + 2) % 6) + 1 FROM students s
UNION ALL
SELECT s.id, (((s.id - 1) + 4) % 6) + 1 FROM students s WHERE s.id % 3 = 0;

-- ===== 6) Goi y tu kiem tra rang buoc sau khi chay =====
-- SELECT email, COUNT(*) FROM students GROUP BY email HAVING COUNT(*) > 1;              -- phai rong (UNIQUE email)
-- SELECT student_id, course_id, COUNT(*) FROM enrollments GROUP BY 1,2 HAVING COUNT(*)>1; -- phai rong (UNIQUE cap)
-- SELECT COUNT(*) FROM students WHERE birth_date > CURRENT_DATE;                          -- phai = 0 (CHECK)
-- SELECT COUNT(*) FROM students WHERE class_id IS NULL;                                   -- so sinh vien chua co lop
