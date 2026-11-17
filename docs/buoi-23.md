# Buổi 23 — Tổng hợp và tự kiểm tra

Câu hỏi tự kiểm tra cuối buổi (theo quy tắc "30 phút cuối buổi thứ Sáu" ở
README), trả lời dựa trên code thật trong repo, không chỉ lý thuyết chung.

## 1. `@Transactional` không rollback khi nào?

Mặc định Spring chỉ rollback với **unchecked exception** (`RuntimeException`/
`Error`), **không rollback** với checked exception (trừ khi khai báo
`rollbackFor`). Ngoài ra không rollback nếu:

- Exception bị **catch và nuốt** bên trong method transactional (Spring
  không thấy exception nào thoát ra để rollback).
- Gọi method `@Transactional` từ **method khác trong cùng class**
  (self-invocation) — proxy Spring bị bỏ qua, transaction không được
  mở/rollback đúng như khai báo.
- Transaction con (`REQUIRES_NEW`) lỗi không tự động rollback transaction
  cha — tùy propagation.

## 2. N+1 xảy ra khi nào? `JOIN FETCH` và `@EntityGraph` khác nhau gì?

N+1 xảy ra khi load danh sách N entity (1 query), sau đó với **mỗi** entity
lại lazy-load thêm 1 quan hệ (N query nữa) — ví dụ `GET /api/classes` load N
lớp rồi mỗi lớp lazy-load `students` riêng → 1 + N SELECT (đã gặp thật ở
buổi 21).

- `JOIN FETCH` (viết trong JPQL `@Query`): fetch ngay trong câu query đó,
  chỉ áp dụng cho đúng query đó, kiểm soát rõ câu SQL sinh ra.
- `@EntityGraph`: khai báo trên repository method, Spring Data tự thêm fetch
  join khi thực thi — tiện khi muốn tái dùng cùng derived query method với
  nhiều "độ sâu" fetch khác nhau, không phải viết tay JPQL. Nhược điểm: khó
  kiểm soát SQL sinh ra bằng mắt, dễ dính `MultipleBagFetchException` nếu
  fetch 2 collection cùng lúc — `JOIN FETCH` thì lỗi đó hiện rõ ngay trong
  JPQL lúc viết.

## 3. Optimistic và pessimistic locking khác nhau thế nào? Xung đột trả status gì?

- **Optimistic** (`@Version`): không khóa gì ở DB lúc đọc, cho phép đọc song
  song; chỉ khi `UPDATE` mới check `WHERE version = x` — nếu version đã đổi
  (request khác ghi trước), update ảnh hưởng 0 dòng → Hibernate ném
  `OptimisticLockException`. Phù hợp khi tỉ lệ xung đột thấp.
- **Pessimistic** (`SELECT ... FOR UPDATE` qua `@Lock(PESSIMISTIC_WRITE)`):
  khóa dòng ngay lúc đọc, request khác phải **chờ** ở mức DB cho tới khi
  transaction đầu commit/rollback. Phù hợp khi tỉ lệ xung đột cao.
- Trong code này: xung đột optimistic (`ObjectOptimisticLockingFailureException`)
  được `GlobalExceptionHandler` bắt và trả **409 Conflict**
  (`exception/GlobalExceptionHandler.java`). Pessimistic thì không có "xung
  đột" dạng exception — request thứ hai chỉ bị **block/chờ** ở DB.

## 4. Vì sao dùng `ddl-auto=validate` khi đã có Flyway?

Vì Flyway mới là nguồn quản lý schema (migration có thứ tự, review được,
chạy lại được trên môi trường khác). Nếu để Hibernate `update`/`create`,
Hibernate có thể tự ý sinh thêm cột/bảng khác với migration đã viết → hai
nguồn sự thật xung đột nhau, schema thực tế trôi dần khỏi những gì migration
mô tả. `validate` buộc Hibernate chỉ **so khớp** entity với schema Flyway đã
tạo, phát hiện sớm nếu entity và migration lệch nhau, nhưng không tự sửa gì.

## 5. Sửa một file migration đã chạy thì chuyện gì xảy ra?

Flyway lưu checksum của mỗi file migration trong bảng
`flyway_schema_history`. Nếu sửa nội dung file (vd `V2__...sql`) đã apply
rồi, lần chạy sau Flyway phát hiện **checksum mismatch** và báo lỗi
`FlywayValidateException`, chặn ứng dụng khởi động — không tự "chạy lại"
migration đó, vì Flyway coi migration đã chạy là bất biến. Muốn sửa thật,
phải tạo migration mới (`V6__...`) hoặc dùng lệnh `repair` (chấp nhận rủi
ro, chỉ dùng khi hiểu rõ hệ quả).

## 6. Read Committed cho phép hiện tượng gì mà Repeatable Read chặn được?

Read Committed cho phép **non-repeatable read**: trong cùng 1 transaction,
đọc lại cùng 1 dòng hai lần có thể thấy giá trị khác nhau vì transaction
khác đã commit thay đổi ở giữa. Repeatable Read chặn hiện tượng này — trong
cùng transaction, đọc lại dòng đó luôn thấy giá trị như lần đọc đầu (snapshot
cố định suốt transaction). Postgres mặc định chạy ở Read Committed.

## 7. Lớp còn 1 chỗ: chọn `FOR UPDATE` hay `@Version`? Vì sao?

Nên chọn **`FOR UPDATE` (pessimistic)**. Đây là tài nguyên giới hạn gần cạn
(1 chỗ) — xác suất 2 request cùng lúc tranh nhau chỗ cuối là **cao**, và với
optimistic, cả hai request đều đọc xong, tính toán xong rồi mới biết thua ở
bước update → tốn công retry. Pessimistic khóa ngay từ đầu, request thứ hai
chờ, sau khi request đầu commit thì đọc được `enrolledCount` mới nhất và
biết ngay lớp đã đầy — tuần tự hóa đúng ý nghĩa nghiệp vụ "ai tới trước được
chỗ trước", tránh race ở tình huống tranh chấp cao như thế này. Đối chiếu
thực tế đã làm ở buổi 22: `addToClass` (optimistic) trả `409` lúc commit,
`addToClassPessimistic` (`ClassRepository.findByIdForUpdate`) chặn ngay lúc
đọc bằng khóa DB, kết quả đo được giống bảng so sánh trong `docs/ai-review-log/buoi-22.md`.
