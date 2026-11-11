-- Buổi 15 — Thứ Ba 24/11/2026 — DB 50 phút
-- Đề bài: PostgreSQL không tự tạo index cho cột khóa ngoại. Sinh thêm vài
-- nghìn sinh viên bằng generate_series; chạy EXPLAIN câu lọc theo class_id,
-- tạo index cho students.class_id, so kết quả trước và sau.
-- Đọc trước: PostgreSQL docs — Using EXPLAIN (phần đầu).

-- ============================================================
-- Phần 0-3: AI viết (theo yêu cầu của người học — "phần này bạn code còn
-- tôi review cũng được"), người học tự chạy + tự đọc kết quả EXPLAIN bên
-- dưới thay vì tự gõ câu lệnh. Ghi lại vào docs/ai-review-log nếu phát
-- hiện chỗ nào AI làm chưa đúng/chưa tối ưu.
-- ============================================================

-- ============================================================
-- Phần 0: Sinh thêm dữ liệu mẫu
-- ============================================================
-- Đang chạy trên DB thật của app (schema do Hibernate sinh từ entity, có
-- thêm cột created_at NOT NULL so với sql/week-3/05-classes.sql) — insert
-- phải kèm created_at vì cột này không có default ở tầng DB.
--
-- Guard: nếu classes đang rỗng, tự seed vài lớp trước — tránh lỗi
-- "division by zero" ở bước dưới khi COUNT(*) = 0.
INSERT INTO classes (name, created_at)
SELECT 'Lop CNTT' || g, CURRENT_TIMESTAMP FROM generate_series(1, 5) AS g
WHERE NOT EXISTS (SELECT 1 FROM classes);

-- Entity Student (sql/entity) cũng có birth_date/created_at NOT NULL không
-- default ở tầng DB (Hibernate set created_at trong Java) -> insert thẳng
-- qua SQL phải tự truyền created_at.
-- full_name tiếng Việt + email dạng ASCII theo đúng tên (cùng bộ mảng
-- Họ/Đệm/Tên đã dùng ở sql/week-3/08-seed-data.sql), nối thêm số thứ tự g
-- vào email để tránh trùng (nhiều người có thể cùng họ tên).
--
-- Guard: chỉ chạy nếu CHƯA có dữ liệu bulk (tránh insert chồng lần 2 gây
-- lỗi trùng email 23505 — email sinh ra xác định (deterministic) theo g,
-- chạy lại nhiều lần sẽ ra lại đúng các email cũ nếu bảng đã trống, nhưng
-- nếu bảng đã có sẵn 5000 dòng này rồi thì sẽ đụng unique constraint).
INSERT INTO students (full_name, email, birth_date, created_at, class_id)
SELECT
    (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Vũ','Đặng','Bùi','Đỗ','Ngô'])[(g % 10) + 1]
        || ' ' || (ARRAY['Văn','Thị'])[(g % 2) + 1]
        || ' ' || (ARRAY['An','Bình','Cường','Đức','Giang','Nam','Mai','Huy','Linh','Long',
                         'Hoa','Hùng','Lan','Nghĩa','Thu','Tùng','Hạnh','Khánh','Nga','Phúc'])[(g % 20) + 1] AS full_name,
    lower(
        (ARRAY['nguyen','tran','le','pham','hoang','vu','dang','bui','do','ngo'])[(g % 10) + 1]
        || '.' ||
        (ARRAY['an','binh','cuong','duc','giang','nam','mai','huy','linh','long',
               'hoa','hung','lan','nghia','thu','tung','hanh','khanh','nga','phuc'])[(g % 20) + 1]
        || g || '@gmail.com'
    ) AS email,
    (DATE '2000-01-01' + ((g * 17) % 3000)) AS birth_date,
    CURRENT_TIMESTAMP AS created_at,
    c.id AS class_id
FROM generate_series(1, 5000) AS g
    JOIN LATERAL (
    SELECT id FROM classes ORDER BY id OFFSET (g % (SELECT COUNT(*) FROM classes)) LIMIT 1
    ) c ON TRUE
WHERE NOT EXISTS (SELECT 1 FROM students WHERE email = 'tran.binh1@gmail.com');

-- Kiểm tra nhanh đã có đủ dữ liệu để EXPLAIN ra kết quả có ý nghĩa:
-- SELECT COUNT(*) FROM students;
-- SELECT class_id, COUNT(*) FROM students GROUP BY class_id ORDER BY class_id;


-- ============================================================
-- Phần 1: EXPLAIN TRƯỚC khi có index trên class_id
-- ============================================================
-- Dự đoán trước khi chạy: với vài nghìn dòng và KHÔNG có index trên
-- class_id, Postgres sẽ chọn Seq Scan hay Index Scan? Vì sao (gợi ý: FK
-- chỉ đảm bảo toàn vẹn dữ liệu, không tự sinh index — khác với PRIMARY KEY
-- luôn có index kèm theo)?
--
-- Lọc theo class_id = 1 (lớp đầu tiên seed ở sql/week-3/08-seed-data.sql;
-- đổi lại số này nếu id lớp thực tế trên máy bạn khác).

EXPLAIN ANALYZE
SELECT id, full_name, email
FROM students
WHERE class_id = 1;

-- Dán kết quả EXPLAIN thực tế vào đây, rồi tự đọc: node đầu tiên là gì
-- (Seq Scan/Index Scan), cost, số dòng ước lượng (rows=...):
--
-- Kết quả:


-- ============================================================
-- Phần 2: Tạo index cho students.class_id
-- ============================================================

CREATE INDEX idx_students_class_id ON students (class_id);


-- ============================================================
-- Phần 3: EXPLAIN SAU khi có index — so sánh
-- ============================================================
-- Chạy lại ĐÚNG câu SELECT ở Phần 1 (cùng class_id), kèm EXPLAIN.

EXPLAIN ANALYZE
SELECT id, full_name, email
FROM students
WHERE class_id = 1;

-- Dán kết quả:
--
-- Kết quả:
--
-- Tự trả lời (ghi vào đây):
-- - Node quét đổi từ gì sang gì?
-- - cost và rows ước lượng thay đổi thế nào?
-- - Nếu KHÔNG thấy đổi (vẫn Seq Scan dù đã có index), thử nghĩ vì sao
--   (gợi ý: bảng sinh_viên đủ nhỏ hay class_id được lọc chiếm tỉ lệ quá lớn
--   trên tổng số dòng — planner có thể vẫn chọn Seq Scan nếu rẻ hơn, đó
--   không phải là bug).


-- ===== Ghi chú / nhật ký (tự điền sau khi làm xong Phần 1-3) =====
