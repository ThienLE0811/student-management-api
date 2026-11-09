# Buổi 10 — Xử lý lỗi chuẩn (ProblemDetail) + Ràng buộc DB

## 1. ProblemDetail / GlobalExceptionHandler (Spring)

- `ProblemDetail` (RFC 7807) là format lỗi chuẩn Spring Boot 3+ hỗ trợ sẵn: `type`, `title`, `status`, `detail`, `instance`, và cho phép thêm field tùy chỉnh qua `setProperty(key, value)`.
- `@RestControllerAdvice` + `@ExceptionHandler` bắt exception tập trung cho toàn bộ Controller, tránh lặp `try/catch` ở từng method.
- Các handler đã làm:
  - `NotFoundException` → 404
  - `DuplicateEmailException` → 409
  - `MethodArgumentNotValidException` → 400, kèm `errors: {field: message}` lấy từ `ex.getBindingResult().getFieldErrors()`
  - `Exception` (catch-all) → 500, **log đầy đủ stack trace ở server** (`log.error(...)`) nhưng **không lộ** message/exception thật ra response, chỉ trả "Internal server error" chung chung.
- Nguyên tắc thiết kế: Service ném exception khi gặp lỗi nghiệp vụ (not found, duplicate), không dùng `Optional`/`boolean` rồi để Controller tự `if/else` quyết định status — giữ Controller mỏng, logic quyết định status tập trung ở GlobalExceptionHandler.

## 2. SQLSTATE khi vi phạm ràng buộc DB (PostgreSQL)

| SQLSTATE | Tên ràng buộc | Khi nào xảy ra |
|---|---|---|
| `23505` | `unique_violation` | Insert/update trùng giá trị ở cột có `UNIQUE`/`PRIMARY KEY` |
| `23503` | `foreign_key_violation` | Tham chiếu khóa chính không tồn tại, hoặc xóa bản ghi cha đang bị bảng con tham chiếu (không có `CASCADE`) |
| `23514` | `check_violation` | Vi phạm điều kiện trong `CHECK (...)` |

Trong Spring, các lỗi này thường bị bọc thành `DataIntegrityViolationException` (cụ thể `DuplicateKeyException` cho `23505`).

## 3. Bảng ánh xạ Validation Java ↔ Ràng buộc DB

| Validation Java | Ràng buộc DB tương ứng | Ghi chú |
|---|---|---|
| `@Email` | Không có ràng buộc DB tương đương | Format email chỉ check ở tầng ứng dụng |
| `@NotBlank` | `NOT NULL` | `NOT NULL` chỉ chặn `null`, không chặn chuỗi rỗng `""` — muốn chặn luôn thì cần thêm `CHECK (length(trim(col)) > 0)` |
| `@Size(max = n)` | `VARCHAR(n)` | DB lỗi nếu insert chuỗi dài hơn khai báo cột |
| `@Past` | `CHECK (birth_date < CURRENT_DATE)` | Vi phạm → `23514` |
| Check trùng email trong Service (`findAll().anyMatch(...)`) | `UNIQUE (email)` | Xem mục 4 — vì sao vẫn cần cả hai |
| FK `class_id` trong `students` | `FOREIGN KEY (class_id) REFERENCES classes(id)` | Vi phạm → `23503` |

## 4. Vì sao vẫn cần `UNIQUE` ở DB dù Service đã check trùng email

Check ở Service (đọc rồi mới ghi) **không atomic** → có khoảng hở giữa bước đọc (`SELECT`) và bước ghi (`INSERT`). Đây là race condition dạng **TOCTOU (time-of-check to time-of-use)**.

Kịch bản 2 request cùng lúc:
1. Request A: `SELECT ... WHERE email = 'x@test.com'` → không thấy → đi tiếp
2. Request B: `SELECT ... WHERE email = 'x@test.com'` → cũng không thấy (A chưa insert xong) → đi tiếp
3. Request A: `INSERT ...` → thành công
4. Request B: `INSERT ...` → **cũng thành công** → 2 bản ghi trùng email lọt vào DB

`UNIQUE` constraint đảm bảo tính atomic ở mức index/row-lock ngay tại thời điểm ghi, nên DB luôn phát hiện trùng (`23505`) bất kể có bao nhiêu request chạy song song — không phụ thuộc vào việc ứng dụng có kịp check trước đó hay không.

→ Kết luận: validate ở Service là để trả lỗi sớm, thân thiện (409 kèm message rõ ràng); `UNIQUE` ở DB là lớp phòng thủ cuối cùng đảm bảo tính đúng đắn dữ liệu. Hai lớp bổ sung cho nhau, không thay thế nhau.

**Việc cần làm thêm (chưa làm):** thêm handler bắt `DataIntegrityViolationException` trong `GlobalExceptionHandler` để convert lỗi `23505` lọt từ DB lên (trường hợp race condition khiến check ở Service bị miss) thành 409, thay vì rơi vào handler `Exception.class` → 500.
