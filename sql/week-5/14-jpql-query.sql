-- Buổi 17 — Thứ Năm 26/11/2026 — DB 45-60 phút
-- Đề bài: Viết SQL thuần trong DBeaver TRƯỚC khi chuyển sang JPQL/@Query
-- trong code (buổi này học JPQL, @Query, native query ở tầng Java — phần SQL
-- thuần ở đây là bước "tư duy trước" để sau so sánh JPQL sinh ra có đúng ý
-- không).
--   1. Điểm trung bình theo khóa (course).
--   2. Sinh viên chưa đăng ký khóa học nào.
-- Dữ liệu dùng lại từ buổi 16 (100.000 dòng enrollments, 5000 students) —
-- không cần sinh thêm sample data cho bài này.

-- ============================================================
-- Phần 1: Điểm trung bình theo khóa
-- ============================================================
-- Gợi ý tự làm:
-- - Bảng nào chứa điểm (score)? Khóa học join qua cột nào?
-- - AVG() bỏ qua NULL hay tính NULL là 0? Nếu muốn hiện cả khóa chưa ai có
--   điểm, GROUP BY cần xuất phát từ courses hay enrollments?
-- - Có nên JOIN courses để lấy course.name thay vì chỉ course_id?

SELECT c.id AS course_id, c.name AS course_name, AVG(e.score) AS average_score
FROM courses c
LEFT JOIN enrollments e ON e.course_id = c.id
GROUP BY c.id, c.name
ORDER BY c.id;
-- LEFT JOIN từ courses (không phải INNER JOIN từ enrollments) để khóa học
-- chưa ai đăng ký/chưa ai có điểm vẫn xuất hiện trong kết quả (average_score
-- sẽ là NULL cho khóa đó — AVG() tự bỏ qua NULL trong các dòng nó tính,
-- không tính NULL = 0).


-- ============================================================
-- Phần 2: Sinh viên chưa đăng ký khóa học nào
-- ============================================================
-- Gợi ý tự làm:
-- - 2 cách thường dùng: NOT EXISTS (subquery) hoặc LEFT JOIN + WHERE
--   enrollments.id IS NULL. Cách nào rõ ràng hơn? Cách nào Postgres tối ưu
--   tốt hơn — thử EXPLAIN ANALYZE cả 2 rồi so sánh.
-- - Có cần DISTINCT không? Vì sao (gợi ý: 1 student có thể có nhiều dòng
--   enrollments, ảnh hưởng gì tới JOIN kiểu LEFT JOIN)?

-- Cách 1: NOT EXISTS
SELECT s.id, s.full_name, s.email
FROM students s
WHERE NOT EXISTS (
    SELECT 1 FROM enrollments e WHERE e.student_id = s.id
)
ORDER BY s.id;

-- Cách 2: LEFT JOIN ... IS NULL
-- Không cần DISTINCT: LEFT JOIN rồi lọc enrollments.id IS NULL chỉ giữ lại
-- các dòng KHÔNG match (student không có enrollments nào), nên mỗi student
-- tối đa xuất hiện 1 lần — DISTINCT chỉ cần thiết nếu JOIN điều kiện match
-- (sinh ra nhiều dòng trùng student do 1-nhiều).
SELECT s.id, s.full_name, s.email
FROM students s
LEFT JOIN enrollments e ON e.student_id = s.id
WHERE e.id IS NULL
ORDER BY s.id;

EXPLAIN ANALYZE
SELECT s.id, s.full_name, s.email
FROM students s
WHERE NOT EXISTS (
    SELECT 1 FROM enrollments e WHERE e.student_id = s.id
);

EXPLAIN ANALYZE
SELECT s.id, s.full_name, s.email
FROM students s
LEFT JOIN enrollments e ON e.student_id = s.id
WHERE e.id IS NULL;
-- TODO: tự chạy 2 EXPLAIN ANALYZE trên, dán kết quả thực tế vào đây rồi so
-- sánh: cùng ra 1 kiểu plan (Postgres thường tối ưu NOT EXISTS thành Anti
-- Join giống hệt LEFT JOIN ... IS NULL) hay khác nhau trên data hiện tại?


-- ===== Ghi chú / nhật ký (điều cần nhớ qua bài này) =====
--
-- 1. AVG() tự bỏ qua NULL trong các dòng nó gộp — không coi NULL như 0, nên
--    LEFT JOIN từ courses là bắt buộc nếu muốn thấy cả khóa chưa có điểm nào
--    (average_score = NULL), không bị "biến mất" khỏi kết quả như khi dùng
--    INNER JOIN/JOIN thường từ enrollments.
-- 2. NOT EXISTS và LEFT JOIN ... IS NULL cùng trả về kết quả giống nhau cho
--    bài "không có dòng liên quan" — khác REQUIRES_NEW/semantics khi bảng
--    phụ (enrollments) có thể có NULL ở cột join, nhưng ở đây student_id là
--    FK NOT NULL nên không gặp bẫy đó.
--
-- TODO: tự chạy 2 EXPLAIN ANALYZE ở Phần 2, dán kết quả thực tế vào đây và
-- tự kết luận: Postgres planner có chọn cùng 1 kiểu Anti Join cho cả 2 câu
-- không, hay khác nhau trên data hiện tại của bạn?


-- ============================================================
-- Phần 3: Window function — ROW_NUMBER, RANK, AVG(...) OVER (PARTITION BY ...)
-- ============================================================
-- Đây là phần sẽ đưa vào native query (JPQL không hỗ trợ window function).
--   3a. Top 3 sinh viên điểm cao nhất MỖI khóa học.
--   3b. So điểm mỗi sinh viên với điểm trung bình của khóa đó (chênh lệch
--       score - AVG(score) cùng khóa).

-- ------------------------------------------------------------
-- 3a. Top 3 sinh viên điểm cao nhất mỗi khóa
-- ------------------------------------------------------------
-- Gợi ý tự làm:
-- - PARTITION BY course_id, ORDER BY score DESC — mỗi khóa được "đánh số"
--   riêng, không lẫn qua khóa khác.
-- - ROW_NUMBER() và RANK() khác nhau thế nào khi có 2 sinh viên BẰNG điểm ở
--   vị trí thứ 3? Dùng cái nào cho đúng ý "top 3" (đúng 3 dòng/khóa, hay có
--   thể nhiều hơn 3 nếu đồng hạng)?
-- - Window function không dùng được trực tiếp trong WHERE/HAVING (được tính
--   ở pha SELECT, sau WHERE/GROUP BY) — cần bọc bằng subquery/CTE rồi lọc
--   ở lớp ngoài, không viết thẳng WHERE row_num <= 3 trên cùng SELECT.

-- ROW_NUMBER(): luôn đúng 3 dòng/khóa dù có đồng điểm ở ranh giới (thứ tự
-- giữa các dòng đồng điểm là tùy ý/không xác định).
WITH ranked AS (
    SELECT
        e.course_id,
        e.student_id,
        e.score,
        ROW_NUMBER() OVER (PARTITION BY e.course_id ORDER BY e.score DESC) AS row_num
    FROM enrollments e
)
SELECT course_id, student_id, score, row_num
FROM ranked
WHERE row_num <= 3
ORDER BY course_id, row_num;

-- RANK(): nếu 2 sinh viên đồng điểm ở vị trí thứ 3, CẢ HAI đều mang rank 3
-- (và không có rank 4 ngay sau — rank nhảy số), nên 1 khóa có thể trả về
-- NHIỀU HƠN 3 dòng khi WHERE rank <= 3 (đúng ý "top 3 MỨC điểm", khác với
-- ROW_NUMBER() là "đúng 3 DÒNG").
WITH ranked AS (
    SELECT
        e.course_id,
        e.student_id,
        e.score,
        RANK() OVER (PARTITION BY e.course_id ORDER BY e.score DESC) AS rnk
    FROM enrollments e
)
SELECT course_id, student_id, score, rnk
FROM ranked
WHERE rnk <= 3
ORDER BY course_id, rnk;
-- Chọn ROW_NUMBER() nếu đề bài muốn "đúng 3 sinh viên/khóa" (bất kể đồng
-- điểm), chọn RANK() nếu muốn "không bỏ sót ai cùng mức điểm top 3".


-- ------------------------------------------------------------
-- 3b. So điểm mỗi sinh viên với điểm trung bình khóa
-- ------------------------------------------------------------
-- Gợi ý tự làm:
-- - AVG(score) OVER (PARTITION BY course_id) khác AVG(score) GROUP BY
--   course_id ở chỗ nào (số dòng trả về, có giữ lại từng dòng enrollment
--   gốc không)?
-- - Cột chênh lệch (score - avg_trong_khoa) tính thế nào trong cùng 1 SELECT
--   có window function?

SELECT
    e.student_id,
    e.course_id,
    e.score,
    AVG(e.score) OVER (PARTITION BY e.course_id) AS avg_score_khoa,
    e.score - AVG(e.score) OVER (PARTITION BY e.course_id) AS chenh_lech
FROM enrollments e
ORDER BY e.course_id, e.student_id;
-- Khác GROUP BY course_id: GROUP BY "gộp" nhiều dòng thành 1 dòng/khóa (mất
-- thông tin từng student). AVG() OVER (PARTITION BY ...) vẫn giữ nguyên số
-- dòng gốc của enrollments — mỗi dòng vẫn là 1 (student, course), chỉ thêm
-- cột phụ avg_score_khoa (giá trị lặp lại giống nhau cho các dòng cùng
-- course_id), nên so sánh trực tiếp score - avg_score_khoa được ngay trên
-- cùng 1 dòng mà không cần tự JOIN lại kết quả GROUP BY.


-- ===== Ghi chú / nhật ký Phần 3 =====
--
-- 1. Window function (ROW_NUMBER/RANK/AVG() OVER) được tính SAU WHERE/GROUP
--    BY, cùng pha với SELECT — nên không thể lọc nó ngay trong WHERE/HAVING
--    của CHÍNH câu SELECT đó; phải bọc CTE/subquery rồi lọc ở lớp ngoài.
-- 2. ROW_NUMBER(): luôn trả đúng N dòng/nhóm (số thứ tự duy nhất, không lặp)
--    — phù hợp "lấy đúng top N dòng". RANK(): các dòng đồng giá trị ORDER BY
--    nhận CÙNG rank và rank bị nhảy số sau đó — phù hợp "lấy top N MỨC giá
--    trị", có thể trả nhiều hơn N dòng nếu có đồng hạng ở ranh giới.
-- 3. AVG(score) OVER (PARTITION BY course_id) khác AVG(score) ... GROUP BY
--    course_id: GROUP BY gộp nhiều dòng thành 1 dòng tổng hợp/nhóm (mất chi
--    tiết từng dòng gốc); OVER (PARTITION BY ...) giữ nguyên số dòng gốc,
--    chỉ "phát" cùng 1 giá trị tổng hợp (ở đây là AVG) vào mọi dòng cùng
--    nhóm — nhờ vậy so sánh score - avg_score_khoa được ngay trên từng dòng
--    enrollment mà không cần tự JOIN lại kết quả GROUP BY.
--
-- TODO: tự chạy các câu trên bằng data thật (buổi 16: 100k enrollments),
-- xác nhận lại 3 điểm trên có đúng với kết quả thực tế không — đặc biệt thử
-- tìm 1 course_id có đồng điểm ở ranh giới top 3 để thấy khác biệt
-- ROW_NUMBER() vs RANK() rõ ràng (nếu data random không có đồng điểm tự
-- nhiên, có thể tự UPDATE vài dòng enrollments.score bằng nhau để test).
