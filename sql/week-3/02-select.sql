-- Buổi 2 — SELECT, WHERE, ORDER BY, LIMIT/OFFSET trên bảng students
-- Tự viết 10 câu truy vấn (không dùng AI), trong đó có:
--   - tìm theo tên (ILIKE)
--   - tìm theo khoảng tuổi (tính từ birth_date)
-- Ghi lại kết quả/nhận xét ngắn trong comment phía dưới mỗi câu nếu cần.

-- 1. Mẫu (AI viết) — tìm sinh viên có tên chứa "an", không phân biệt hoa thường
SELECT * FROM students
WHERE full_name ILIKE '%an%'
ORDER BY full_name;


-- 2. AND + BETWEEN
SELECT * FROM students
WHERE birth_date BETWEEN '2003-01-01' AND '2004-12-31'
  AND email LIKE '%@gmail.com';


-- 3.OR
SELECT * FROM students
WHERE full_name ILIKE '%minh%' OR full_name ILIKE '%huy%';


-- 4.In
SELECT * FROM students
WHERE id IN (1, 3, 5, 7);


-- 5.Tính tuổi từ birth_date + so sánh — sinh viên trên 21 tuổi
SELECT full_name, birth_date,
       EXTRACT(YEAR FROM AGE(CURRENT_DATE, birth_date)) AS age
FROM students
WHERE EXTRACT(YEAR FROM AGE(CURRENT_DATE, birth_date)) > 21
ORDER BY age DESC;


-- 6. Khoảng tuổi bằng BETWEEN — tuổi từ 20 đến 22
SELECT full_name, birth_date
FROM students
WHERE EXTRACT(YEAR FROM AGE(CURRENT_DATE, birth_date)) BETWEEN 20 AND 22
ORDER BY birth_date;


-- 7.IS NULL
SELECT * FROM students
WHERE email IS NULL;


-- 8.ORDER BY
SELECT * FROM students
ORDER BY birth_date DESC, full_name ASC;


-- 9.LIMIT
SELECT * FROM students
ORDER BY created_at DESC
    LIMIT 3;

-- 10. LIMIT + OFFSET
SELECT * FROM students
ORDER BY id
    LIMIT 3 OFFSET 3;