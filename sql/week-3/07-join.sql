-- Buoi 7: INNER JOIN, LEFT JOIN
-- Doc truoc: SQLBolt bai 6-8

-- 1) INNER JOIN: sinh vien kem ten lop (chi lay sinh vien DA co lop)
-- students INNER JOIN classes ...
SELECT *
FROM students
         INNER JOIN classes ON students.class_id = classes.id;

-- 2) LEFT JOIN: sinh vien CHUA co lop (lop IS NULL ben phia classes)
-- students LEFT JOIN classes ... WHERE ... IS NULL
SELECT *
FROM students
         LEFT JOIN classes ON students.class_id = classes.id  WHERE classes.id IS NULL;

-- 3) Cac khoa hoc cua MOT sinh vien cu the (qua bang trung gian enrollments)
-- students JOIN enrollments JOIN courses ... WHERE students.id = ?
SELECT *
FROM students
         JOIN enrollments ON students.id = enrollments.student_id
         JOIN courses ON enrollments.course_id = courses.id
         WHERE students.id = 4;


-- 4) Moi khoa hoc co nhung ai (liet ke sinh vien theo tung course)
-- courses JOIN enrollments JOIN students ...
SELECT *
FROM courses
         JOIN enrollments ON courses.id = enrollments.course_id
         JOIN students ON students.id = enrollments.student_id
ORDER BY courses.id;

-- ===== Ghi chu / nhat ky =====
-- (dien loi gap phai, dieu chinh khi lam xong)
