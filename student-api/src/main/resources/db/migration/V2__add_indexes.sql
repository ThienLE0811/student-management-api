-- Buổi 19 — Flyway V2: 2 index đã tạo thủ công ở tuần 5 (sql/week-5), nay đưa
-- vào migration để DB mới khởi tạo từ đầu cũng có đủ index như DB dev hiện tại.

-- Buổi 15 (sql/week-5/12-index-fk.sql): Postgres không tự tạo index cho FK,
-- query lọc students theo class_id (vd StudentController getByClassId) cần
-- index riêng trên cột này.
CREATE INDEX idx_students_class_id ON students (class_id);

-- Buổi 16 (sql/week-5/13-enrollments-multicolumn-index.sql): UNIQUE
-- (student_id, course_id) có kèm 1 index ngầm nhưng course_id đứng SAU trong
-- tổ hợp cột đó nên không tận dụng được theo leftmost prefix rule khi lọc
-- riêng course_id (vd trang "danh sách sinh viên của 1 khóa học") -> cần
-- thêm 1 index riêng với course_id đứng trước.
CREATE INDEX idx_enrollments_course_id_student_id ON enrollments (course_id, student_id);
