-- Buổi 11 — Subquery, EXISTS/NOT EXISTS, IN, CTE (WITH)

-- 1. Sinh viên chưa đăng ký khóa nào — cách 1: LEFT JOIN ... IS NULL
-- Gợi ý: LEFT JOIN students với enrollments qua student_id, lọc enrollments.student_id IS NULL
SELECT students.*
FROM students
LEFT JOIN enrollments ON students.id = enrollments.student_id
WHERE enrollments.student_id IS NULL;

-- 2. Sinh viên chưa đăng ký khóa nào — cách 2: NOT EXISTS
-- Gợi ý: WHERE NOT EXISTS (SELECT 1 FROM enrollments WHERE enrollments.student_id = students.id)
SELECT students.*
FROM students
WHERE NOT EXISTS (SELECT 1 FROM enrollments WHERE enrollments.student_id = students.id);

-- 3. Sinh viên chưa đăng ký khóa nào — cách 3: NOT IN
-- Gợi ý: WHERE students.id NOT IN (SELECT student_id FROM enrollments)
SELECT students.*
FROM students
WHERE students.id NOT IN (SELECT student_id FROM enrollments);

-- 4. Lớp chưa có sinh viên — dùng NOT IN (SELECT class_id FROM students)
-- Gợi ý: WHERE classes.id NOT IN (SELECT class_id FROM students)
-- Dự đoán trước khi chạy: kết quả có đúng là các lớp trống không, hay rỗng bất thường? Vì sao?
SELECT classes.*
FROM classes
WHERE classes.id NOT IN (SELECT class_id FROM students);


-- 5. Sửa câu 4 cho đúng bằng NOT EXISTS (hoặc lọc bỏ NULL trong subquery)
-- Gợi ý: NOT EXISTS (SELECT 1 FROM students WHERE students.class_id = classes.id)
SELECT classes.*
FROM classes
WHERE NOT EXISTS (SELECT 1 FROM students WHERE students.class_id = classes.id);

-- 6. (tùy chọn) Viết lại 1 trong các câu trên bằng CTE (WITH ... AS (...))
WITH EnrolledStudents AS (
    SELECT DISTINCT student_id
    FROM enrollments
)
SELECT s.*
FROM students s
WHERE NOT EXISTS (
    SELECT 1
    FROM EnrolledStudents e
    WHERE e.student_id = s.id
);

-- ===== Ghi chú / nhật ký =====
-- Câu 4 (NOT IN) ra rỗng dù thực tế có lớp trống, vì students.class_id có NULL
-- (sinh viên chưa xếp lớp). "NOT IN" so x với CẢ danh sách cùng lúc, tương đương
-- chuỗi AND (x <> v1 AND x <> v2 AND ... AND x <> NULL). So với NULL luôn ra
-- UNKNOWN, không phải TRUE/FALSE, nên cả chuỗi AND không thể nào là TRUE được
-- -> WHERE loại bỏ mọi dòng -> kết quả rỗng, bất kể lớp đó có trống thật hay không.
--
-- Câu 5 (NOT EXISTS) không bị bẫy này vì nó so sánh TỪNG DÒNG một
-- (students.class_id = classes.id), không gộp thành một danh sách rồi so 1 lần.
-- Nếu một dòng students có class_id NULL thì chỉ riêng dòng đó không khớp
-- (NULL = classes.id -> UNKNOWN -> bị loại khỏi subquery), không ảnh hưởng gì
-- đến việc kiểm tra các dòng students khác hay các lớp khác.
--
-- Rút ra: khi cột trong subquery của NOT IN có thể NULL, luôn ưu tiên NOT EXISTS,
-- hoặc phải lọc NULL thủ công: NOT IN (SELECT class_id FROM students WHERE class_id IS NOT NULL).
