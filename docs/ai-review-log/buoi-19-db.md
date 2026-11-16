# Buổi 19 (DB song song) — `sql/week-6/15-pagination.sql`

Đề bài: Phân trang LIMIT/OFFSET và phân trang theo khóa (keyset: `WHERE id >
? ORDER BY id LIMIT 20`); chi phí của `COUNT(*)`. So `EXPLAIN ANALYZE` giữa
`OFFSET 50000` với cách theo khóa.

## Override — AI viết thẳng (có xin phép)

Theo đúng quy ước "DB song song" (`CLAUDE.md`), AI mặc định chỉ sinh sample
data, để phần EXPLAIN/query lại TODO cho người học tự gõ. Đã hỏi lại trước
khi làm, người học chọn **override lần này** ("AI viết thẳng cả query/EXPLAIN,
tôi review") — giống pattern đã dùng ở buổi 15/16/17.

## Phát hiện: lần đo đầu tiên của AI bị sai do stats cũ ngay sau bulk insert

AI chạy `EXPLAIN ANALYZE` **ngay sau** khi vừa bulk insert xong 100.000 dòng
(Phần 0), chưa đợi/tự `ANALYZE`. Kết quả lúc đó:

- `OFFSET 50000 LIMIT 20` → **Parallel Seq Scan + Sort** (external merge,
  tràn đĩa), Execution Time **92.373 ms** — node Seq Scan ước lượng
  `rows=9583` dù thực tế là 100.000 (stats cũ, bảng coi như gần rỗng).
- `WHERE id > 50000 LIMIT 20` → Index Scan, **0.174 ms**.
- `COUNT(*)` → Seq Scan, **25.315 ms**.

Người học tự chạy lại ĐÚNG câu `OFFSET 50000` trong DBeaver vài phút sau và
báo kết quả **khác hẳn**: Index Scan, chỉ **13.5 ms**, không phải Seq Scan +
Sort — phát hiện này đúng, không phải do người học chạy sai. AI kiểm tra lại
`pg_stat_user_tables.last_autoanalyze`: autovacuum đã tự chạy `ANALYZE` xen
giữa 2 lần đo, cập nhật lại thống kê bảng (estimate đúng `rows=100000`), nên
planner đổi sang chọn Index Scan.

**Đã sửa `sql/week-6/15-pagination.sql`**: thêm `ANALYZE students;` tường
minh ngay sau Phần 0 (không trông chờ autovacuum), chạy lại toàn bộ Phần 1-3
bằng stats đã đúng, và thêm hẳn 1 mục "Bài học phụ" ở cuối file giải thích rõ
hiện tượng này (kèm giữ lại số liệu SAI ban đầu để đối chiếu, không xóa đi).

## Kết quả cuối cùng (sau khi `ANALYZE`, đã verify 2 lần, số liệu ổn định)

- **Phần 1 (OFFSET)**: dùng Index Scan (không cần Sort nữa nhờ có index),
  nhưng vẫn phải DUYỆT QUA toàn bộ phần OFFSET trước khi cắt — `OFFSET 50000`:
  **9.796 ms**, `OFFSET 90000`: **43.564 ms** (tăng theo độ sâu trang, dù
  cùng dùng Index Scan).
- **Phần 2 (keyset)**: `WHERE id > 50000`: **0.150 ms**, `WHERE id > 90000`:
  **0.100 ms** — gần như hằng số bất kể vị trí.
- So trực tiếp: nhanh hơn ~**65 lần** ở offset 50000, ~**435 lần** ở offset
  90000 — càng sâu trang, keyset càng áp đảo rõ hơn.
- **Phần 3 (`COUNT(*)`)**: sau khi bảng đã VACUUM, planner chọn **Index Only
  Scan** trên `idx_students_class_id` (`Heap Fetches: 0`), **11.280 ms** —
  rẻ hơn Seq Scan lúc đầu, nhưng vẫn tỉ lệ thuận với tổng số dòng (không có
  COUNT O(1) chính xác).
- Ghi chú liên hệ ngược lại Phần 1 buổi 19 (`GET /api/students` dùng
  `Pageable`): cách Spring Data `page`/`size` đang dùng tương đương
  LIMIT/OFFSET — sẽ chậm dần nếu nhảy trang rất xa trên bảng lớn; keyset hợp
  hơn cho kiểu UI "load more"/infinite scroll.

## Build & test

Không liên quan code Java/Maven — chỉ chạy SQL trực tiếp qua
`docker exec -i student-api-postgres psql -U student_api -d student_api`.

## Người học tự làm

- Đọc lại `sql/week-6/15-pagination.sql`, đặc biệt mục "Bài học phụ" — hiểu
  vì sao benchmark ngay sau bulk insert/update lớn có thể cho kết quả KHÔNG
  đại diện cho hệ thống ở trạng thái ổn định, và vì sao luôn nên tự
  `ANALYZE`/`VACUUM` trước khi đo thật.
- Đối chiếu lại điều kiện để `COUNT(*)` dùng được Index Only Scan (bảng đã
  VACUUM, có ít nhất 1 index B-tree đầy đủ) — không phải lúc nào `COUNT(*)`
  cũng là Seq Scan như AI từng nói sai ở lần đầu.
- Cân nhắc có nên đổi `GET /api/students` sang keyset pagination không (đánh
  đổi: mất khả năng nhảy thẳng tới "trang số X") — chưa làm, ngoài phạm vi
  buổi này, để tự quyết ở buổi sau nếu thấy cần.
