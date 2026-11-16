-- Buổi 19 — Thứ Hai 30/11/2026 — DB 45-60 phút
-- Đề bài: Phân trang LIMIT/OFFSET và phân trang theo khóa (keyset pagination:
-- WHERE id > ? ORDER BY id LIMIT 20); chi phí của COUNT(*). So EXPLAIN
-- ANALYZE giữa OFFSET 50000 với cách theo khóa.
-- Đọc trước: PostgreSQL docs — LIMIT/OFFSET, "keyset pagination" (seek method).

-- ============================================================
-- Phần 0-4: AI viết thẳng (theo yêu cầu của người học — "override lần này,
-- AI viết thẳng cả query/EXPLAIN, tôi review"), ĐÃ tự chạy thật trên Postgres
-- dev (container student-api-postgres, DB vừa được tạo lại sạch ở phần
-- Flyway buổi này — `docker compose down -v`) — kết quả EXPLAIN dưới đây là
-- SỐ THẬT, đã dán nguyên log psql. Lưu ý quan trọng về 1 lần "số liệu đổi
-- giữa 2 lần chạy" — xem mục riêng cuối file.
-- ============================================================

-- ============================================================
-- Phần 0: Sinh dữ liệu mẫu — 100.000 sinh viên
-- ============================================================
-- Cần đủ lớn để OFFSET 50000 có ý nghĩa (nửa bảng) — tái dùng đúng pattern
-- generate_series đã dùng ở sql/week-5/12-index-fk.sql (buổi 15), đổi prefix
-- email "buoi19.sv" để không đụng data cũ nếu DB chưa bị xóa.
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
        'buoi19.sv' || g || '.' ||
        (ARRAY['nguyen','tran','le','pham','hoang','vu','dang','bui','do','ngo'])[(g % 10) + 1]
        || '.' ||
        (ARRAY['an','binh','cuong','duc','giang','nam','mai','huy','linh','long',
               'hoa','hung','lan','nghia','thu','tung','hanh','khanh','nga','phuc'])[(g % 20) + 1]
        || '@gmail.com'
    ) AS email,
    (DATE '2000-01-01' + ((g * 17) % 3000)) AS birth_date,
    CURRENT_TIMESTAMP AS created_at,
    c.id AS class_id
FROM generate_series(1, (100000 - (SELECT COUNT(*) FROM students))::int) AS g
    JOIN LATERAL (
    SELECT id FROM classes ORDER BY id OFFSET (g % (SELECT COUNT(*) FROM classes)) LIMIT 1
    ) c ON TRUE
WHERE (SELECT COUNT(*) FROM students) < 100000;

-- Kết quả thật đã chạy: 100.000 sinh viên, id từ 1 đến 100.000 (liên tục,
-- không có khoảng trống — DB vừa tạo lại từ đầu bằng Flyway nên identity bắt
-- đầu từ 1).
-- SELECT COUNT(*) FROM students;    -> 100000
-- SELECT MIN(id), MAX(id) FROM students;  -> min=1, max=100000

-- QUAN TRỌNG trước khi đo hiệu năng: sau khi bulk insert xong, nên tự chạy
-- `ANALYZE students;` (và đợi hoặc tự `VACUUM students;`) trước khi đo
-- EXPLAIN ANALYZE thật — xem mục "Bài học phụ" ở cuối file để hiểu vì sao
-- bỏ qua bước này có thể khiến kết quả đo bị sai lệch hoàn toàn.
ANALYZE students;


-- ============================================================
-- Phần 1: Phân trang LIMIT/OFFSET — trang "xa" (OFFSET lớn)
-- ============================================================
-- Giả lập việc người dùng bấm thẳng tới trang 2501 (size=20): trang đó bắt
-- đầu từ dòng thứ 50001 -> OFFSET 50000 LIMIT 20. Thử thêm OFFSET 90000 để
-- thấy rõ xu hướng tăng theo độ sâu trang.

EXPLAIN ANALYZE
SELECT id, full_name, email
FROM students
ORDER BY id
OFFSET 50000 LIMIT 20;

-- Kết quả thật (sau khi đã ANALYZE, stats đúng):
-- Limit  (cost=1988.29..1989.09 rows=20 width=59) (actual time=9.707..9.716 rows=20 loops=1)
--   ->  Index Scan using students_pkey on students  (cost=0.29..3976.29 rows=100000 width=59) (actual time=0.029..7.901 rows=50020 loops=1)
-- Planning Time: 0.247 ms
-- Execution Time: 9.796 ms

EXPLAIN ANALYZE
SELECT id, full_name, email
FROM students
ORDER BY id
OFFSET 90000 LIMIT 20;

-- Kết quả thật:
-- Limit  (cost=3578.69..3579.49 rows=20 width=59) (actual time=43.348..43.467 rows=20 loops=1)
--   ->  Index Scan using students_pkey on students  (cost=0.29..3976.29 rows=100000 width=59) (actual time=0.047..40.050 rows=90020 loops=1)
-- Planning Time: 0.075 ms
-- Execution Time: 43.564 ms
--
-- Đọc kết quả: lần này planner DÙNG Index Scan trên `students_pkey` (không
-- phải Seq Scan như lần đo đầu tiên khi stats còn cũ — xem "Bài học phụ"
-- cuối file). Nhưng dù dùng index, Postgres vẫn phải DUYỆT TUẦN TỰ qua đúng
-- OFFSET+LIMIT dòng đầu tiên trong B-tree rồi mới vứt bỏ phần OFFSET, giữ lại
-- LIMIT dòng cuối ("actual rows=50020"/"rows=90020" ở node Index Scan, dù
-- câu trả về cuối cùng chỉ có 20 dòng) — Index Scan ở đây giúp khỏi phải SẮP
-- XẾP lại (đã có sẵn thứ tự `id` từ B-tree), nhưng không giúp "nhảy thẳng"
-- bỏ qua phần OFFSET. Vì vậy OFFSET 90000 (43.564 ms) vẫn chậm hơn rõ rệt so
-- với OFFSET 50000 (9.796 ms) dù cùng 1 bảng, cùng dùng Index Scan — chi phí
-- tăng gần tuyến tính theo giá trị OFFSET.


-- ============================================================
-- Phần 2: Phân trang theo khóa (keyset pagination)
-- ============================================================
-- Cùng mục tiêu "lấy 20 dòng tiếp theo sau vị trí tương đương trang trước"
-- nhưng thay vì OFFSET, dùng "con trỏ" (cursor) là id của dòng cuối trang
-- trước — so ở đúng 2 vị trí đã đo ở Phần 1 (50000 và 90000) để công bằng.

EXPLAIN ANALYZE
SELECT id, full_name, email
FROM students
WHERE id > 50000
ORDER BY id
LIMIT 20;

-- Kết quả thật:
-- Limit  (cost=0.29..1.14 rows=20 width=59) (actual time=0.088..0.095 rows=20 loops=1)
--   ->  Index Scan using students_pkey on students  (cost=0.29..2119.91 rows=50035 width=59) (actual time=0.086..0.091 rows=20 loops=1)
--         Index Cond: (id > 50000)
-- Planning Time: 0.178 ms
-- Execution Time: 0.150 ms

EXPLAIN ANALYZE
SELECT id, full_name, email
FROM students
WHERE id > 90000
ORDER BY id
LIMIT 20;

-- Kết quả thật:
-- Limit  (cost=0.29..1.15 rows=20 width=59) (actual time=0.052..0.057 rows=20 loops=1)
--   ->  Index Scan using students_pkey on students  (cost=0.29..430.99 rows=10097 width=59) (actual time=0.050..0.054 rows=20 loops=1)
--         Index Cond: (id > 90000)
-- Planning Time: 0.096 ms
-- Execution Time: 0.100 ms
--
-- Đọc kết quả: Index Cond `(id > N)` cho Postgres "nhảy thẳng" tới đúng vị
-- trí N trong B-tree rồi đọc tiếp 20 dòng liền kề — KHÔNG phải duyệt qua
-- N dòng trước đó như Phần 1. "actual rows" ở node Index Scan đúng bằng 20
-- (so với 50020/90020 ở Phần 1) — đây chính là khác biệt cốt lõi.
--
-- So sánh trực tiếp (cùng 1 bảng 100k dòng, cùng vị trí trang):
--   OFFSET 50000 LIMIT 20      -> Execution Time:  9.796 ms
--   WHERE id > 50000 LIMIT 20  -> Execution Time:  0.150 ms   (~65 lần)
--   OFFSET 90000 LIMIT 20      -> Execution Time: 43.564 ms
--   WHERE id > 90000 LIMIT 20  -> Execution Time:  0.100 ms   (~435 lần)
-- => Keyset nhanh hơn RÕ RỆT ở cả 2 vị trí, và mức chênh lệch CÀNG LỚN khi
-- offset càng sâu (435 lần ở offset 90000 so với 65 lần ở offset 50000) —
-- đúng với lý thuyết: OFFSET tăng chi phí tuyến tính theo độ sâu trang, còn
-- keyset gần như hằng số (~0.1-0.15ms) bất kể đang ở vị trí nào trong bảng.


-- ============================================================
-- Phần 3: Chi phí của COUNT(*)
-- ============================================================
-- Trang phân trang kiểu "hiện tổng N trang" (vd UI có nút nhảy tới trang
-- cuối) thường cần COUNT(*) tổng số dòng — đo chi phí của chính câu COUNT.

EXPLAIN ANALYZE
SELECT COUNT(*) FROM students;

-- Kết quả thật (sau khi bảng đã được VACUUM — xem "Bài học phụ" cuối file):
-- Aggregate  (cost=2078.29..2078.30 rows=1 width=8) (actual time=11.206..11.207 rows=1 loops=1)
--   ->  Index Only Scan using idx_students_class_id on students  (cost=0.29..1828.29 rows=100000 width=0) (actual time=0.258..7.162 rows=100000 loops=1)
--         Heap Fetches: 0
-- Planning Time: 1.371 ms
-- Execution Time: 11.280 ms
--
-- Đọc kết quả: COUNT(*) KHÔNG LUÔN LUÔN là Seq Scan — ở đây planner tự chọn
-- quét `idx_students_class_id` (index hẹp nhất đang có, không phải PK) bằng
-- Index Only Scan, "Heap Fetches: 0" nghĩa là KHÔNG CẦN mở bảng heap ra kiểm
-- tra visibility của bất kỳ dòng nào — vì bảng đã được VACUUM, visibility map
-- xác nhận toàn bộ trang dữ liệu "all-visible". Điều kiện để có được điều
-- này: (1) có sẵn ít nhất 1 index B-tree đầy đủ trên bảng (ở đây là
-- `idx_students_class_id`, không cần chứa đúng cột liên quan tới COUNT,
-- chỉ cần tồn tại toàn bộ dòng), và (2) bảng đã VACUUM gần đây. Nếu bảng vừa
-- bulk insert/update nhiều mà CHƯA VACUUM, Postgres vẫn phải Seq Scan +
-- kiểm tra visibility từng dòng qua heap (xem "Bài học phụ" — lần đo đầu
-- tiên của chính câu COUNT(*) này đã ra đúng Seq Scan).
-- Dù vậy, COUNT(*) chính xác (dù Index Only Scan hay Seq Scan) vẫn luôn tỉ
-- lệ thuận với TỔNG số dòng trong bảng — không có cách "đếm tức thời O(1)"
-- nào cho số chính xác. Nếu chỉ cần hiển thị "khoảng X dòng" (không bắt buộc
-- chính xác), `SELECT reltuples::bigint FROM pg_class WHERE relname =
-- 'students'` đọc thống kê ước lượng có sẵn, gần như tức thời, không quét
-- bảng — đánh đổi độ chính xác lấy tốc độ.


-- ============================================================
-- Bài học phụ: stats cũ ngay sau bulk insert làm sai lệch kết quả đo
-- ============================================================
-- Lần đo ĐẦU TIÊN (ngay sau khi Phần 0 vừa insert xong 100.000 dòng, CHƯA
-- kịp autovacuum chạy ANALYZE/VACUUM) cho ra kết quả KHÁC HẲN:
--   OFFSET 50000 LIMIT 20     -> Parallel Seq Scan + Sort (external merge,
--                                tràn đĩa), Execution Time: 92.373 ms
--   WHERE id > 50000 LIMIT 20 -> Index Scan, Execution Time: 0.174 ms
--   COUNT(*)                  -> Seq Scan, Execution Time: 25.315 ms
-- Node Seq Scan lúc đó ước lượng "rows=9583" trong khi thực tế là 100.000 —
-- planner dùng thống kê CŨ (bảng coi như gần rỗng) để tính chi phí, nên chọn
-- NHẦM kế hoạch rẻ hơn trên giấy nhưng đắt hơn thực tế.
--
-- Sau khi người học tự chạy lại câu Phần 1 trong DBeaver (cách nhau vài
-- phút), autovacuum đã kịp chạy (`pg_stat_user_tables.last_autoanalyze` xác
-- nhận), thống kê cập nhật đúng rows=100000 -> planner đổi sang Index Scan,
-- Execution Time giảm xuống ~9-13ms. Đây là lý do file này giờ có thêm lệnh
-- `ANALYZE students;` tường minh ngay sau Phần 0, thay vì trông chờ
-- autovacuum tự chạy kịp lúc.
--
-- Bài học: khi benchmark hiệu năng ngay sau 1 lần bulk insert/update lớn,
-- LUÔN tự chạy `ANALYZE <table>;` (và cân nhắc `VACUUM <table>;` nếu cần đo
-- cả Index Only Scan) trước khi đo — nếu không, kết quả đo được phản ánh
-- hành vi của planner với thống kê SAI, không đại diện cho hệ thống ở trạng
-- thái bình thường (đã chạy ổn định, autovacuum đã bắt kịp).


-- ===== Ghi chú / nhật ký (điều cần nhớ qua bài này) =====
--
-- 1. OFFSET N không "nhảy tới" được vị trí N trong bảng — kể cả khi có index
--    và planner dùng Index Scan (tránh được bước Sort), nó vẫn phải DUYỆT
--    QUA đúng N+LIMIT dòng đầu tiên theo thứ tự rồi mới vứt bỏ N dòng đầu.
--    Chi phí tăng gần tuyến tính theo N dù có index hay không — index chỉ
--    giúp khỏi phải Sort, không giúp "bỏ qua" OFFSET.
-- 2. Keyset pagination (WHERE <cột_order_by> > <giá_trị_cuối_trang_trước>
--    ORDER BY <cột_order_by> LIMIT N) tận dụng Index Cond để nhảy thẳng tới
--    đúng vị trí cursor — chi phí gần như hằng số bất kể đang ở trang đầu
--    hay trang thứ 2500. Đánh đổi: không nhảy thẳng tới "trang số X" tùy ý
--    được (chỉ đi tiếp/lùi tuần tự từ 1 cursor), khác OFFSET có thể nhảy tới
--    bất kỳ trang nào.
-- 3. COUNT(*) không có cách đếm O(1) cho số CHÍNH XÁC — luôn tỉ lệ thuận với
--    tổng số dòng, dù planner có thể chọn Index Only Scan (rẻ hơn Seq Scan)
--    NẾU bảng đã VACUUM (visibility map all-visible) và có index phù hợp.
--    Nếu UI chỉ cần số gần đúng, `pg_class.reltuples` nhanh hơn nhiều.
-- 4. Thống kê planner (từ ANALYZE) có thể bị CŨ ngay sau 1 đợt bulk
--    insert/update lớn — luôn tự `ANALYZE` trước khi benchmark, nếu không
--    kết quả đo được có thể phản ánh 1 kế hoạch mà Postgres bình thường sẽ
--    không bao giờ chọn.
-- 5. Áp dụng vào app (liên hệ buổi 19 Phần 1 — `GET /api/students` dùng
--    `Pageable`): endpoint hiện tại dùng cách tương đương LIMIT/OFFSET (qua
--    `page`/`size` của Spring Data `Pageable`) — đúng, dễ dùng, hỗ trợ nhảy
--    trang bất kỳ, nhưng sẽ CHẬM DẦN nếu người dùng nhảy tới trang rất xa
--    trên bảng rất lớn. Keyset pagination phù hợp hơn cho kiểu UI "infinite
--    scroll"/"load more" (không cần nhảy tới trang số X cụ thể), đánh đổi
--    lấy hiệu năng ổn định ở mọi độ sâu trang.
