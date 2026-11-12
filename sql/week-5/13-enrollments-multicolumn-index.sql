-- Buổi 16 — Thứ Tư 25/11/2026 — DB 45-60 phút
-- Đề bài: Sinh 100.000 dòng enrollments bằng generate_series. Index nhiều
-- cột (student_id, course_id): thứ tự cột ảnh hưởng thế nào khi lọc chỉ
-- theo course_id? Đọc EXPLAIN ANALYZE: Seq Scan và Index Scan.
-- Đọc trước: PostgreSQL docs — Multicolumn Indexes (phần "Index-Only Scans"
-- có thể bỏ qua, chỉ cần phần "Column order" + "Combining Multiple Indexes").

-- ============================================================
-- Phần 0 (AI sinh dữ liệu mẫu, theo đúng ngoại lệ "DB song song" trong README)
-- là seed đủ courses/students để có thể sinh 100.000 dòng enrollments (ràng
-- buộc UNIQUE(student_id, course_id) ở sql/week-3/06-schema.sql / entity
-- Enrollment nghĩa là cần SỐ SINH VIÊN × SỐ KHÓA HỌC >= 100.000 cặp khác
-- nhau). Từ Phần 1 trở đi là phần tự làm.
-- ============================================================

-- ------------------------------------------------------------
-- Phần 0a: Seed thêm khóa học (nếu chưa đủ)
-- ------------------------------------------------------------
-- Guard bằng tên khóa học đánh dấu, tránh insert trùng nếu chạy lại file
-- nhiều lần (courses.name đang UNIQUE ở entity Course).
INSERT INTO courses (name, created_at)
SELECT 'Mon hoc ' || g, CURRENT_TIMESTAMP
FROM generate_series(1, 30) AS g
WHERE NOT EXISTS (SELECT 1 FROM courses WHERE name = 'Mon hoc ' || g);

-- ------------------------------------------------------------
-- Phần 0b: Seed thêm sinh viên (nếu chưa đủ 5000) — tái dùng đúng pattern đã
-- dùng ở sql/week-5/12-index-fk.sql. Guard bằng COUNT(*) tổng (không dùng
-- email đánh dấu cố định như file 12, vì dữ liệu buổi trước có thể đã seed
-- theo pattern email khác — dùng email cố định làm guard sẽ không nhận ra
-- bảng đã đủ dữ liệu và insert chồng thêm, như đã xảy ra khi AI tự chạy thử
-- file này (ghi lại ở docs/ai-review-log/buoi-16.md)).
-- ------------------------------------------------------------
INSERT INTO classes (name, created_at)
SELECT 'Lop CNTT' || g, CURRENT_TIMESTAMP FROM generate_series(1, 5) AS g
WHERE NOT EXISTS (SELECT 1 FROM classes);

INSERT INTO students (full_name, email, birth_date, created_at, class_id)
SELECT
    (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Vũ','Đặng','Bùi','Đỗ','Ngô'])[(g % 10) + 1]
        || ' ' || (ARRAY['Văn','Thị'])[(g % 2) + 1]
        || ' ' || (ARRAY['An','Bình','Cường','Đức','Giang','Nam','Mai','Huy','Linh','Long',
                         'Hoa','Hùng','Lan','Nghĩa','Thu','Tùng','Hạnh','Khánh','Nga','Phúc'])[(g % 20) + 1] AS full_name,
    lower(
        'buoi16.sv' || g || '.' ||
        (ARRAY['nguyen','tran','le','pham','hoang','vu','dang','bui','do','ngo'])[(g % 10) + 1]
        || '.' ||
        (ARRAY['an','binh','cuong','duc','giang','nam','mai','huy','linh','long',
               'hoa','hung','lan','nghia','thu','tung','hanh','khanh','nga','phuc'])[(g % 20) + 1]
        || '@gmail.com'
    ) AS email,
    (DATE '2000-01-01' + ((g * 17) % 3000)) AS birth_date,
    CURRENT_TIMESTAMP AS created_at,
    c.id AS class_id
FROM generate_series(1, (5000 - (SELECT COUNT(*) FROM students))::int) AS g
    JOIN LATERAL (
    SELECT id FROM classes ORDER BY id OFFSET (g % (SELECT COUNT(*) FROM classes)) LIMIT 1
    ) c ON TRUE
WHERE (SELECT COUNT(*) FROM students) < 5000;

-- ------------------------------------------------------------
-- Phần 0c: Sinh 100.000 dòng enrollments
-- ------------------------------------------------------------
-- CROSS JOIN students x courses rồi LIMIT 100.000 — mỗi cặp (student_id,
-- course_id) xuất hiện tối đa 1 lần nên không đụng UNIQUE constraint; thêm
-- ON CONFLICT DO NOTHING để file chạy lại nhiều lần vẫn an toàn (lần chạy
-- sau có thể insert được ÍT HƠN 100.000 dòng mới nếu phần lớn cặp đã tồn
-- tại từ lần chạy trước — kiểm tra lại bằng SELECT COUNT(*) FROM enrollments
-- sau khi chạy).
-- Lưu ý cú pháp: LIMIT thuộc về phần SELECT (áp dụng cho số dòng ỨNG VIÊN
-- trước khi xét trùng), còn ON CONFLICT là mệnh đề của INSERT, PHẢI đứng
-- SAU toàn bộ query (kể cả LIMIT) — thứ tự ngược lại sẽ bị Postgres hiểu sai
-- (AI tự chạy thử bị lỗi "duplicate key" ở đúng chỗ này, xem
-- docs/ai-review-log/buoi-16.md).
INSERT INTO enrollments (student_id, course_id, score, created_at)
SELECT s.id, c.id, round((random() * 10)::numeric, 1), CURRENT_TIMESTAMP
FROM students s
CROSS JOIN courses c
LIMIT 100000
ON CONFLICT (student_id, course_id) DO NOTHING;

-- Kiểm tra nhanh đã có đủ dữ liệu để EXPLAIN ra kết quả có ý nghĩa:
-- SELECT COUNT(*) FROM enrollments;
-- SELECT course_id, COUNT(*) FROM enrollments GROUP BY course_id ORDER BY course_id LIMIT 5;


-- ============================================================
-- Phần 1: EXPLAIN ANALYZE TRƯỚC khi có index nhiều cột
-- ============================================================
-- Bảng enrollments hiện tại CHỈ có UNIQUE(student_id, course_id) — mà theo
-- CLAUDE.md/buổi 15, index trên FK không tự sinh, chỉ UNIQUE constraint mới
-- kéo theo 1 index ngầm (trên đúng tổ hợp cột của UNIQUE).
--
-- Lọc theo course_id = 1 (đổi lại id thực tế trên máy bạn nếu khác):

EXPLAIN ANALYZE
SELECT id, student_id, score
FROM enrollments
WHERE course_id = 1;

-- Dự đoán trước khi chạy: UNIQUE(student_id, course_id) hiện có kéo theo
-- 1 B-tree index ngầm trên (student_id, course_id) — theo "leftmost prefix
-- rule" của multicolumn index, index đó có tận dụng được để lọc CHỈ theo
-- course_id (không kèm student_id) không? Vì sao?
--
-- Kết quả thực tế (chưa có index đúng thứ tự cột cho course_id):
-- Seq Scan on enrollments  (cost=0.00..2917.00 rows=3267 width=24)
--                          (actual time=0.011..10.277 rows=3226 loops=1)
--   Filter: (course_id = 1)
--   Rows Removed by Filter: 96774
-- Planning Time: 1.209 ms
-- Execution Time: 10.419 ms
--
-- => Đúng dự đoán: index ngầm (student_id, course_id) KHÔNG dùng được để
--    lọc riêng course_id (course_id là cột thứ 2 của index, không phải cột
--    đầu) -> Postgres phải Seq Scan toàn bộ ~100.000 dòng, loại bỏ 96.774
--    dòng không khớp.


-- ============================================================
-- Phần 2: Tạo index nhiều cột khác — tự chọn thứ tự cột
-- ============================================================
-- Tạo 1 index mới (student_id, course_id) hoặc (course_id, student_id) —
-- tự quyết định thứ tự dựa trên câu hỏi: nếu mục tiêu là tăng tốc truy vấn
-- lọc THEO COURSE_ID (như Phần 1), cột nào nên đứng TRƯỚC trong index?

CREATE INDEX idx_enrollments_course_id_student_id
    ON enrollments (course_id, student_id);
-- course_id đứng TRƯỚC vì truy vấn mục tiêu (Phần 1) lọc riêng course_id —
-- phải khớp leftmost prefix rule thì index mới dùng được cho lọc đơn cột.


-- ============================================================
-- Phần 3: EXPLAIN ANALYZE SAU khi có index — so sánh
-- ============================================================
-- Chạy lại ĐÚNG câu SELECT ở Phần 1 (cùng course_id), kèm EXPLAIN ANALYZE.

EXPLAIN ANALYZE
SELECT id, student_id, score
FROM enrollments
WHERE course_id = 1;

-- Kết quả thực tế (sau khi có idx_enrollments_course_id_student_id):
-- Bitmap Heap Scan on enrollments  (cost=77.74..1793.40 rows=3267 width=24)
--                                  (actual time=1.822..3.418 rows=3226 loops=1)
--   Recheck Cond: (course_id = 1)
--   Heap Blocks: exact=835
--   ->  Bitmap Index Scan on idx_enrollments_course_id_student_id
--         (cost=0.00..76.92 rows=3267 width=0)
--         (actual time=1.567..1.590 rows=3226 loops=1)
--         Index Cond: (course_id = 1)
-- Planning Time: 6.562 ms
-- Execution Time: 4.858 ms
--
-- Tự trả lời:
-- - Node quét đổi từ Seq Scan -> Bitmap Heap Scan + Bitmap Index Scan (không
--   phải Index Scan "thuần" vì số dòng khớp course_id=1 đủ nhiều — ~3226/835
--   block -> Postgres chọn gom theo block (bitmap) cho hiệu quả, thay vì
--   Index Scan tuần tự từng dòng).
-- - "Rows Removed by Filter: 96774" đã BIẾN MẤT — không còn phải loại bỏ
--   dòng thừa, vì Bitmap Index Scan đã xác định đúng 835 block chứa dữ liệu
--   cần lấy ngay từ index, không cần duyệt hết bảng.
-- - Execution Time giảm từ 10.419 ms -> 4.858 ms (khoảng 2.1 lần, với
--   100.000 dòng; chênh lệch sẽ càng rõ hơn với bảng lớn hơn nhiều).
-- - Nếu tạo index SAI thứ tự (student_id, course_id) rồi lọc riêng course_id:
--   không dùng được theo leftmost prefix rule như ở Phần 1 (đã kiểm chứng
--   bằng chính index ngầm từ UNIQUE(student_id, course_id) — course_id đứng
--   sau, không tận dụng được, vẫn ra Seq Scan).


-- ============================================================
-- Phần 4: So sánh với cách lọc theo student_id
-- ============================================================
-- Thử lại Phần 1-3 nhưng đổi điều kiện lọc sang WHERE student_id = <id>
-- thay vì course_id. Với CÙNG 1 index nhiều cột (student_id, course_id),
-- lần này kết quả EXPLAIN có khác gì so với lọc theo course_id không?

-- TODO: tự chạy + tự trả lời
--
-- Trả lời:


-- ===== Ghi chú / nhật ký (điều cần nhớ qua bài này) =====
--
-- 1. Index nhiều cột chỉ hiệu quả theo "leftmost prefix rule": index
--    (A, B) dùng tốt cho lọc theo A, hoặc (A và B) cùng lúc, nhưng KHÔNG
--    dùng được (hoặc dùng rất kém) nếu chỉ lọc riêng B một mình — Postgres
--    cần nhìn thấy cột đầu tiên khớp điều kiện mới "đi tiếp" được vào index.
--
-- 2. => Thứ tự cột trong CREATE INDEX phải chọn theo QUERY THỰC TẾ hay dùng
--    để lọc, không phải chọn tùy ý hay theo thứ tự khai báo cột trong bảng.
--    Nếu ứng dụng thường lọc theo course_id một mình (vd trang "danh sách
--    sinh viên của 1 khóa học"), course_id phải đứng TRƯỚC trong index —
--    như đã làm ở Phần 2 (idx_enrollments_course_id_student_id).
--
-- 3. UNIQUE constraint tự tạo kèm 1 index ngầm, nhưng thứ tự cột của
--    constraint đó (student_id, course_id) được quyết định bởi ý nghĩa
--    ràng buộc dữ liệu (không được trùng cặp), KHÔNG PHẢI vì mục đích tối
--    ưu truy vấn — 2 mục tiêu này có thể cần 2 index khác nhau, như trường
--    hợp ở đây (vẫn giữ UNIQUE(student_id, course_id) để chặn trùng, tạo
--    THÊM 1 index riêng (course_id, student_id) để tối ưu lọc course_id).
--
-- 4. Đọc EXPLAIN ANALYZE: "cost=X..Y" là ƯỚC LƯỢNG của planner (chưa chạy
--    thật), "actual time=X..Y" mới là thời gian ĐO THẬT; "rows=" trong
--    cost là ước lượng, "rows=" trong actual là số dòng thực khớp. Node
--    "Seq Scan" = quét toàn bộ bảng + lọc sau; "Bitmap Index/Heap Scan" hoặc
--    "Index Scan" = dùng index để tìm thẳng vị trí dữ liệu cần, nhanh hơn
--    khi điều kiện lọc chọn ra 1 phần nhỏ trong tổng số dòng (độ chọn lọc
--    cao).
--
-- 5. Không phải lúc nào có index cũng dùng Index Scan "thuần" — khi số
--    dòng khớp điều kiện đủ nhiều (ở đây ~3.2% tổng bảng), Postgres chọn
--    Bitmap Scan (gom theo block) vì rẻ hơn Index Scan tuần tự từng dòng.
--    Đây là quyết định của query planner dựa trên chi phí ước lượng, không
--    phải lỗi hay thiếu tối ưu.
--
-- (Phần 4 — so sánh lọc theo student_id — tự làm thêm nếu muốn khép lại đầy
-- đủ bài tập; không bắt buộc để hiểu trọn các điểm 1-5 ở trên.)
