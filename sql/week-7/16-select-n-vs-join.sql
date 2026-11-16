-- Buổi 21 — Thứ Tư 02/12/2026 — DB 45-60 phút
-- Đề bài: So 1.000 câu SELECT nhỏ với 1 câu JOIN trả cùng dữ liệu: thời gian
-- chạy và EXPLAIN ANALYZE.
-- Liên hệ: đây chính là phiên bản SQL thuần của lỗi N+1 vừa tạo/sửa ở phần
-- Spring (GET /api/classes) buổi này — thay vì đo qua Hibernate, đo trực
-- tiếp ở tầng Postgres để thấy rõ chi phí đến từ đâu.

-- ============================================================
-- Phần 0-4: AI viết thẳng (theo yêu cầu của người học — "override lần này,
-- AI viết thẳng cả query/EXPLAIN, tôi review"), ĐÃ tự chạy thật trên Postgres
-- dev (container student-api-postgres) — kết quả dưới đây là SỐ THẬT, đã
-- dán nguyên log psql.
-- ============================================================

-- ============================================================
-- Phần 0: Sinh dữ liệu mẫu — 1.000 lớp, mỗi lớp 5 sinh viên (5.000 sinh viên)
-- ============================================================
-- Cần đủ 1.000 lớp để "1.000 câu SELECT nhỏ" có nghĩa thật (1 SELECT/lớp).
-- Tái dùng pattern generate_series đã dùng ở sql/week-5, sql/week-6, đổi
-- prefix 'Buoi21-Lop'/'buoi21.sv' để không đụng data cũ (lớp/sinh viên của
-- các buổi/thí nghiệm trước vẫn còn nguyên trong DB dev).

INSERT INTO classes (name, created_at)
SELECT 'Buoi21-Lop' || g, CURRENT_TIMESTAMP
FROM generate_series(1, 1000) AS g
WHERE NOT EXISTS (SELECT 1 FROM classes WHERE name = 'Buoi21-Lop1');

INSERT INTO students (full_name, email, birth_date, created_at, class_id)
SELECT
    (ARRAY['Nguyễn','Trần','Lê','Phạm','Hoàng','Vũ','Đặng','Bùi','Đỗ','Ngô'])[(g % 10) + 1]
        || ' ' || (ARRAY['Văn','Thị'])[(g % 2) + 1]
        || ' ' || (ARRAY['An','Bình','Cường','Đức','Giang','Nam','Mai','Huy','Linh','Long',
                         'Hoa','Hùng','Lan','Nghĩa','Thu','Tùng','Hạnh','Khánh','Nga','Phúc'])[(g % 20) + 1] AS full_name,
    lower('buoi21.sv' || g || '@gmail.com') AS email,
    (DATE '2000-01-01' + ((g * 17) % 3000)) AS birth_date,
    CURRENT_TIMESTAMP AS created_at,
    c.id AS class_id
FROM generate_series(1, 5000) AS g
    JOIN LATERAL (
        SELECT id FROM classes WHERE name LIKE 'Buoi21-Lop%'
        ORDER BY id OFFSET (g % 1000) LIMIT 1
    ) c ON TRUE
WHERE NOT EXISTS (SELECT 1 FROM students WHERE email = 'buoi21.sv1@gmail.com');

-- Cập nhật thống kê ngay sau bulk insert — bài học rút ra từ buổi 19 (stats
-- cũ ngay sau bulk insert khiến planner chọn nhầm kế hoạch, số đo sai lệch).
ANALYZE classes;
ANALYZE students;

-- Kết quả thật đã chạy:
--   classes: 1000 dòng tên 'Buoi21-Lop1'..'Buoi21-Lop1000'
--   students: 5000 dòng (5 sinh viên/lớp), email 'buoi21.sv1'..'buoi21.sv5000'


-- ============================================================
-- Phần 1: EXPLAIN ANALYZE của 1 câu SELECT nhỏ đại diện
-- ============================================================
-- Trước khi đo tổng 1.000 câu, xem chi phí của ĐÚNG 1 câu trong số đó —
-- đây là truy vấn mà N+1 sẽ lặp lại 1.000 lần (1 lần/lớp).

EXPLAIN ANALYZE
SELECT id, full_name, email
FROM students
WHERE class_id = (SELECT id FROM classes WHERE name = 'Buoi21-Lop1');

-- Kết quả thật:
-- Bitmap Heap Scan on students  (cost=13.58..401.65 rows=129 width=58) (actual time=0.055..0.062 rows=5 loops=1)
--   Recheck Cond: (class_id = $0)
--   Heap Blocks: exact=5
--   InitPlan 1 (returns $0)
--     ->  Index Scan using uk_classes_name on classes  (cost=0.28..8.29 rows=1 width=8) (actual time=0.029..0.029 rows=1 loops=1)
--           Index Cond: ((name)::text = 'Buoi21-Lop1'::text)
--   ->  Bitmap Index Scan on idx_students_class_id  (cost=0.00..5.26 rows=129 width=0) (actual time=0.049..0.049 rows=5 loops=1)
--         Index Cond: (class_id = $0)
-- Planning Time: 0.977 ms
-- Execution Time: 0.124 ms
--
-- Đọc kết quả: 1 câu rất rẻ (~0.12ms thực thi, ~1ms cả planning), nhờ có
-- sẵn idx_students_class_id (tạo từ buổi 15, sql/week-5/12-index-fk.sql) —
-- planner dùng Bitmap Index Scan thay vì Index Scan đơn giản vì planner ước
-- lượng rows=129 (dựa trên thống kê trung bình số sinh viên/lớp của TOÀN BỘ
-- bảng — bảng students lúc này có 105.000 dòng tích luỹ từ các buổi trước,
-- không chỉ riêng 5.000 dòng buổi này), dù thực tế lớp 'Buoi21-Lop1' chỉ có
-- đúng 5 sinh viên. Vấn đề của N+1 không nằm ở chỗ "1 câu chậm" — mà ở chỗ
-- phải trả giá PLANNING (~1ms) + ROUND-TRIP tới DB **1.000 lần liên tiếp**,
-- cộng dồn lại.


-- ============================================================
-- Phần 2: Tổng thời gian chạy 1.000 câu SELECT nhỏ (lặp, không JOIN)
-- ============================================================
-- Dùng DO block (PL/pgSQL) để lặp đúng 1.000 lần, đo tổng thời gian server-
-- side bằng clock_timestamp() — đây là cận dưới lý tưởng (chưa tính network
-- round-trip thật giữa app và DB qua từng câu riêng lẻ như Hibernate làm ở
-- phần N+1 bên Spring; nếu tính cả round-trip qua mạng, chênh lệch với JOIN
-- còn lớn hơn số đo dưới đây).

DO $$
DECLARE
    start_ts timestamptz := clock_timestamp();
    cls_id   bigint;
    cnt      int;
BEGIN
    FOR cls_id IN SELECT id FROM classes WHERE name LIKE 'Buoi21-Lop%' LOOP
        SELECT count(*) INTO cnt FROM students WHERE class_id = cls_id;
    END LOOP;
    RAISE NOTICE 'Tong thoi gian 1000 SELECT nho: % ms',
        extract(milliseconds FROM clock_timestamp() - start_ts);
END $$;

-- Kết quả thật (log NOTICE từ psql):
-- NOTICE:  Tong thoi gian 1000 SELECT nho: 67.706 ms


-- ============================================================
-- Phần 3: EXPLAIN ANALYZE của 1 câu JOIN duy nhất — trả cùng dữ liệu
-- ============================================================
-- Thay vì 1.000 lần "lớp -> SELECT riêng students", lấy toàn bộ 1.000 lớp +
-- 5.000 sinh viên của chúng trong ĐÚNG 1 câu.

EXPLAIN ANALYZE
SELECT c.id AS class_id, c.name AS class_name, s.id AS student_id, s.full_name, s.email
FROM classes c
JOIN students s ON s.class_id = c.id
WHERE c.name LIKE 'Buoi21-Lop%';

-- Kết quả thật:
-- Hash Join  (cost=33.12..2789.91 rows=105000 width=79) (actual time=0.610..15.267 rows=5000 loops=1)
--   Hash Cond: (s.class_id = c.id)
--   ->  Seq Scan on students s  (cost=0.00..2480.00 rows=105000 width=66) (actual time=0.005..5.658 rows=105000 loops=1)
--   ->  Hash  (cost=20.56..20.56 rows=1005 width=21) (actual time=0.359..0.360 rows=1000 loops=1)
--         Buckets: 1024  Batches: 1  Memory Usage: 61kB
--         ->  Seq Scan on classes c  (cost=0.00..20.56 rows=1005 width=21) (actual time=0.006..0.171 rows=1000 loops=1)
--               Filter: ((name)::text ~~ 'Buoi21-Lop%'::text)
--               Rows Removed by Filter: 5
-- Planning Time: 1.286 ms
-- Execution Time: 15.493 ms
--
-- Đọc kết quả: planner chọn Hash Join — Seq Scan CẢ 2 bảng, vì điều kiện
-- lọc là `LIKE 'Buoi21-Lop%'` trên cột `name` (không có index hỗ trợ pattern
-- này) và vế `students` không lọc gì cả (join điều kiện mới xác định quan
-- hệ, không phải WHERE trực tiếp trên students) nên planner quét toàn bộ
-- bảng students — lúc này có tới 105.000 dòng tích luỹ từ các buổi trước
-- (không chỉ 5.000 dòng buổi này), nên Seq Scan trên students chiếm phần
-- lớn thời gian (actual time=...5.658 trong tổng 15.267). Dù planner phải
-- quét Seq Scan cả 2 bảng (không tối ưu bằng việc có sẵn index đúng cho
-- pattern lọc), tổng thời gian lấy về đủ 5.000 dòng kết quả vẫn chỉ 15.493ms
-- — so với 67.706ms của 1.000 lần SELECT rời (Phần 2) — **nhanh hơn ~4.4
-- lần**, NGAY CẢ KHI JOIN này đang ở tình huống bất lợi (phải Seq Scan toàn
-- bảng 105k dòng do thiếu index cho LIKE). Nếu lọc theo điều kiện có index
-- hỗ trợ (vd theo khoảng id thay vì LIKE tên), khoảng cách sẽ còn lớn hơn.
-- Đây cũng là minh chứng cho việc chi phí thật của N+1 không nằm ở "mỗi
-- query riêng lẻ chậm" (Phần 1 cho thấy 1 câu chỉ ~0.1-1ms) — mà nằm ở
-- việc CỘNG DỒN 1.000 lần round-trip + planning, điều mà 1 câu JOIN duy
-- nhất tránh được hoàn toàn dù plan của nó không phải lúc nào cũng tối ưu
-- nhất có thể.


-- ============================================================
-- Phần 4: Tổng kết so sánh
-- ============================================================
-- | Cách                          | Số câu SQL | Thời gian (server-side) |
-- |--------------------------------|-----------:|-------------------------:|
-- | 1.000 SELECT nhỏ (Phần 2)      |       1.000 |                 67.706 ms |
-- | 1 câu JOIN (Phần 3)             |           1 |                  15.493 ms |
--
-- Bài học:
-- 1. Chênh lệch ~4.4 lần CHỈ TÍNH chi phí thực thi trong Postgres (cùng 1
--    session, qua DO block server-side, KHÔNG qua network) — đây là cận
--    DƯỚI của khoảng cách thật. Khi app gọi qua JDBC/Hibernate (như phần
--    N+1 ở Spring buổi này), mỗi câu SELECT còn cộng thêm round-trip TCP +
--    thời gian xử lý ở tầng driver/ORM cho TỪNG lần gọi riêng — khoảng cách
--    thực tế giữa "N+1" và "1 JOIN" trong ứng dụng thật LUÔN lớn hơn con số
--    đo được thuần ở DB như ở đây.
-- 2. Đáng chú ý: câu JOIN ở Phần 3 đang ở tình huống BẤT LỢI (phải Seq Scan
--    toàn bộ bảng students 105.000 dòng vì thiếu index cho điều kiện lọc
--    LIKE trên classes.name) — vậy mà vẫn nhanh hơn hẳn 1.000 SELECT nhỏ
--    (vốn mỗi câu đều tận dụng được index idx_students_class_id). Điều này
--    cho thấy ưu thế của JOIN không chỉ đến từ "query riêng rẻ hay đắt" mà
--    chủ yếu đến từ việc GIẢM SỐ LẦN round-trip + planning từ 1.000 xuống 1.
-- 3. Liên hệ trực tiếp buổi này: N+1 ở GET /api/classes (phần Spring) chính
--    là hiện tượng "1.000 SELECT nhỏ" này nhưng ở quy mô nhỏ hơn (5 lớp = 6
--    câu SQL) — JOIN FETCH/@EntityGraph đã dùng để sửa chính là cách ép
--    Hibernate sinh ra ĐÚNG 1 câu JOIN như Phần 3, thay vì N+1 câu như Phần 2.
