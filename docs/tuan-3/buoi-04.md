# Buổi 4 — @RequestBody, ResponseEntity, status code

- Ngày học: 09/11/2026
- Thời gian thực tế:

## Phần 1 — POST nhận JSON bằng @RequestBody, quan sát Jackson chuyển JSON ↔ object

- Tạo `record StudentRequest(String fullName, String email, LocalDate birthDate)` — chỉ chứa field client cần gửi, không có `id`/`created_at` (2 field này server tự sinh, để ở bước sau khi dùng `ResponseEntity`).
- Khác với `Student` (đặt tên field snake_case: `full_name`, `birth_date`), `StudentRequest` cố tình đặt camelCase (`fullName`, `birthDate`) cho đúng convention chuẩn Java — nghĩa là JSON gửi lên phải dùng key camelCase, không dùng key của `Student`.
- `@PostMapping("/api/students")` nhận `@RequestBody StudentRequest`, tạm thời chỉ echo lại chính object nhận được để quan sát Jackson, chưa add vào `STUDENTS` (việc add/sinh `id`/`created_at` là phần của buổi sau khi học `ResponseEntity`).

### Các case đã thử

| Input `birthDate` | Kết quả | Giải thích |
|---|---|---|
| `"2001-11-5"` | 400 Bad Request | `LocalDate` parse theo chuẩn ISO-8601 nghiêm ngặt `yyyy-MM-dd` — tháng/ngày phải đủ 2 chữ số (đệm số 0). `"2001-11-5"` thiếu số 0 ở ngày → `DateTimeParseException` → Spring trả 400 trước khi vào được method controller. |
| `"hai mươi tuổi"` | 400 Bad Request | Chuỗi không parse được thành `LocalDate` theo bất kỳ format nào → cùng loại lỗi parse như trên. |
| `123` (number, không phải string) | **200 OK**, ra `"1970-05-04"` — không lỗi! | Module `jackson-datatype-jsr310` xử lý `java.time`: khi gặp JSON **number** (khác với string) cho field `LocalDate`, mặc định hiểu con số đó là "epoch day" — số ngày tính từ mốc `1970-01-01`. `123` ngày sau `1970-01-01` ra đúng `1970-05-04`. |

### Tự giải thích

- Lỗi kiểu string sai định dạng ngày → bị Spring chặn ngay (400), an toàn.
- Lỗi kiểu number (ví dụ client gõ nhầm, gửi số thay vì chuỗi ngày) → **không bị chặn**, Jackson âm thầm convert thành một ngày hoàn toàn vô nghĩa (1970) mà không có cảnh báo gì. Đây là loại lỗi nguy hiểm hơn lỗi 400 vì không ai biết sai cho tới khi nhìn vào dữ liệu thực tế — lý do vì sao sau này cần thêm validation (`@Valid` / check thủ công) thay vì chỉ dựa vào việc Jackson parse được hay không.

## Phần 2 — ResponseEntity: status code cho từng endpoint

- `GET /api/students/{id}`: `Optional<Student>` lấy từ `stream().filter().findFirst()`, trả thẳng bằng `ResponseEntity.of(optional)` — tự động 200 kèm body nếu có, 404 (body rỗng) nếu không có. Gọn hơn hẳn việc tự viết if/else.
- `POST /api/students`: cần sinh `id` mới (lấy `id` lớn nhất hiện có trong `STUDENTS` + 1), ghép `StudentRequest` + `id` + `created_at = LocalDateTime.now()` thành `Student` mới, add vào `STUDENTS` (đã đổi từ `List.of(...)` sang `new ArrayList<>(List.of(...))` để add được). Trả `ResponseEntity.created(location).body(newStudent)` — `created(URI)` là shortcut tương đương `status(HttpStatus.CREATED).location(uri)`, tự set sẵn status 201 + header `Location`. `URI` của resource mới lấy qua `ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(newId).toUri()`.
- `PUT /api/students/{id}`: `id` lấy từ `@PathVariable`, không cần trong body — tái dùng `StudentRequest` làm body giống POST (PUT thay thế toàn bộ resource, không phải cập nhật từng phần như PATCH). Vì `Student` là record (immutable) nên không sửa field trực tiếp được — phải tìm `index` của sinh viên trong `STUDENTS` (helper `findIndex(id)` dùng `IntStream`), tạo `Student` mới giữ nguyên `id`/`created_at` gốc, rồi `STUDENTS.set(index, updated)` để thay thế. Không tìm thấy → `ResponseEntity.notFound().build()` (404), tìm thấy → `ResponseEntity.ok(updated)` (200).
- `DELETE /api/students/{id}`: dùng lại `findIndex(id)`, không tìm thấy → 404, tìm thấy → `STUDENTS.remove(index)` (gọi đúng overload `remove(int)` xóa theo vị trí — lưu ý `remove(Object)` sẽ xóa theo `.equals()` và không tương thích kiểu `Long` với `List<Student>`) rồi trả `ResponseEntity.noContent().build()` (204, không có body).

### Tự giải thích

- Các method tĩnh của `ResponseEntity` (`ok()`, `notFound()`, `created()`, `noContent()`) chỉ là shortcut dễ đọc cho 1 status code cụ thể — không có gì "tự động đoán" cả, mỗi method tương ứng 1-1 với 1 status, mình chủ động chọn gọi method nào.
- PUT (thay thế toàn bộ resource, field nào cũng bắt buộc) khác PATCH (cập nhật một phần, field nào không gửi thì giữ nguyên) — bài này chỉ làm PUT theo đúng yêu cầu.

## Phần 3 — DB: UPDATE, DELETE, RETURNING, thói quen BEGIN...ROLLBACK

File: `sql/week-3/04-update-delete.sql`

- `UPDATE ... WHERE id = ... RETURNING *;` và `DELETE ... WHERE id = ... RETURNING *;` — `RETURNING *` cho thấy ngay (các) row vừa bị ảnh hưởng trong cùng 1 câu lệnh, không cần `SELECT` riêng để kiểm tra.
- Thói quen an toàn: bọc câu thử nghiệm trong `BEGIN; ... ROLLBACK;`. `BEGIN` mở transaction (thay đổi chỉ tạm thời, "nháp" trong phạm vi session); `ROLLBACK` hủy toàn bộ, dữ liệu quay lại y hệt trước `BEGIN`; `COMMIT` mới chính thức lưu vĩnh viễn. Transaction gắn với session/connection đang mở — vẫn chạy tiếp các lệnh `ROLLBACK`/`COMMIT` sau bao lâu cũng được, miễn không đóng session, nhưng phải cùng 1 tab/connection với lúc `BEGIN`.
- Thử case quên `WHERE` trên cột `email` (`UPDATE students SET email = 'thienlv@gmail.com';`) → bị chặn ngay bởi ràng buộc `UNIQUE (email)` (`SQL state 23505`, duplicate key), không update được dòng nào, vì gán cùng 1 email cho nhiều dòng vi phạm `UNIQUE`.
- Thử lại case quên `WHERE` trên cột `full_name` (không có `UNIQUE`) (`UPDATE students SET full_name = 'Lê Văn Thiên';`) → **không có lỗi gì**, toàn bộ các dòng trong bảng bị đổi tên giống hệt nhau.

### Tự giải thích

- Ràng buộc DB (`UNIQUE`, `CHECK`...) chỉ bảo vệ được những cột có ràng buộc tương ứng — không phải lưới an toàn chung cho mọi câu lệnh quên `WHERE`. Case `email` may mắn bị `UNIQUE` chặn lại, nhưng case `full_name` thì không — dữ liệu bị phá âm thầm, không có cảnh báo nào.
- Vì vậy thói quen `BEGIN...ROLLBACK` mới là biện pháp phòng ngừa áp dụng được cho **mọi** trường hợp, bất kể cột đó có constraint hay không — không nên ỷ lại vào constraint để tránh lỗi quên `WHERE`.

## Vướng mắc / câu hỏi khi làm

-

## Nhật ký lỗi AI gặp phải (nếu có hỏi AI giải thích lỗi)

| Lỗi/thông báo | AI giải thích gì | Mình tự rút ra điều gì |
|---------------|-------------------|--------------------------|
| `400 Bad Request` khi POST với `"birthDate": "2001-11-5"` | `LocalDate` parse theo ISO-8601 nghiêm ngặt, cần đủ 2 chữ số cho tháng/ngày | Phải cẩn thận format ngày tháng khi test API, không phải lỗi code |
| `"birthDate": 123` → ra `"1970-05-04"` mà không báo lỗi | Jackson hiểu number là "epoch day" tính từ 1970-01-01 | Lỗi kiểu dữ liệu không phải lúc nào cũng bị chặn — cần có validation riêng, không dựa hết vào Jackson |
| `duplicate key value violates unique constraint "students_email_key"` khi `UPDATE` quên `WHERE` trên cột `email` | Ràng buộc `UNIQUE` chặn vì gán cùng email cho nhiều dòng cùng lúc | Ràng buộc DB chỉ bảo vệ được cột có ràng buộc, không phải lưới an toàn chung — vẫn phải cẩn thận `WHERE` |
| Hỏi `ResponseEntity.of(newStudent)` vì sao sai | `ResponseEntity.of()` chỉ nhận `Optional<T>`, không nhận `T` trực tiếp | Cần phân biệt method nào dùng cho case "có thể rỗng" (`of(Optional)`) và case "chắc chắn có" (`created()`, `ok()`) |
| Hỏi `END` có thay được `COMMIT` không | `END` là alias của `COMMIT` trong Postgres, không liên quan `BEGIN...END` của PL/pgSQL | Cùng 1 từ khóa có thể mang nghĩa khác nhau tùy ngữ cảnh, cần để ý |