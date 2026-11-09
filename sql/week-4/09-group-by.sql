-- Buổi 9 — GROUP BY, hàm tổng hợp, HAVING

-- 1. Sĩ số mỗi lớp
-- Gợi ý: JOIN classes với students, GROUP BY lớp, đếm số sinh viên
SELECT classes.id , COUNT(students.id) AS students
FROM classes
         JOIN students ON classes.id = students.class_id
         GROUP BY classes.id;
-- 2. Điểm trung bình mỗi khóa học
-- Gợi ý: GROUP BY course_id trên bảng enrollments, dùng AVG(score)

-- (vùng giao AI — sinh dữ liệu mẫu) score đang NULL hết, gán điểm ngẫu nhiên 0-10 để luyện AVG
UPDATE enrollments
SET score = round((random() * 10)::numeric, 1);


SELECT enrollments.course_id , AVG(enrollments.score) AS average_score

FROM enrollments
GROUP BY enrollments.course_id;

-- 3. Các lớp có từ 10 sinh viên trở lên
-- Gợi ý: giống câu 1, thêm HAVING để lọc theo số lượng
SELECT classes.id , COUNT(students.id) AS so_sinh_vien
FROM classes
JOIN students ON classes.id = students.class_id
GROUP BY classes.id
HAVING COUNT(students.id) > 10;