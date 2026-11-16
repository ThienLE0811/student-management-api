# Buổi 20 — Thứ Ba 01/12/2026 — @Transactional

Phần DB (SQL thuần, 45-60 phút): BEGIN/COMMIT/ROLLBACK/SAVEPOINT, ACID, tắt
auto-commit trong DBeaver, chuyển lớp bằng 2 câu UPDATE + rollback giữa
chừng, quan sát từ cửa sổ thứ 2 trước/sau commit — người học tự gõ, AI chỉ
hướng dẫn bước (đúng quy ước "DB song song", không viết SQL thay).

## Phần Spring (70 phút): PUT /api/students/{id}/class/{classId}

Yêu cầu: chuyển lớp cho sinh viên + ghi 1 dòng lịch sử vào bảng
`class_transfer_history` (migration `V3__class_transfer_history.sql`), đặt
`@Transactional` ở tầng Service, `readOnly = true` cho các hàm chỉ đọc.

### Code (AI viết, theo yêu cầu)

- `db/migration/V3__class_transfer_history.sql`: bảng `class_transfer_history`
  (`student_id`, `from_class_id`, `to_class_id`, `transferred_at`) + 3 FK về
  `students`/`classes`. Không đụng `V1`/`V2`.
- `entity/ClassTransferHistory.java`: `@ManyToOne` tới `Student` và 2 lần
  `SchoolClass` (`fromClass`/`toClass`) — cùng style `Enrollment` (camelCase
  field + `@Column`/`@JoinColumn` tường minh), khác với `Student`/`SchoolClass`
  cũ đang dùng tên field theo cột DB (`full_name`, `birth_date`).
- `repository/ClassTransferHistoryRepository.java`: `JpaRepository` trơn,
  chưa cần query riêng.
- `StudentService`:
  - `transferClass(studentId, classId)`: tìm student + lớp mới (404 nếu
    không có), lưu lại lớp cũ, `save` student, tạo + `save` 1 dòng
    `ClassTransferHistory`. Đặt `@Transactional` (bắt buộc — 2 lệnh `save`
    phải cùng thành công/thất bại).
  - `create`, `update`, `deleteById`: thêm `@Transactional` — không bắt buộc
    vì mỗi method hiện chỉ có 1 lệnh ghi (repository tự bọc transaction
    ngầm), nhưng đặt boundary ở Service là thói quen đúng kiến trúc, phòng
    trường hợp sau này có thêm bước ghi thứ 2 mà quên bọc.
  - `getAll`, `getById`, `getByClassId`, `getWithoutCourse`: thêm
    `@Transactional(readOnly = true)` — tối ưu (tắt dirty-checking), không
    liên quan rollback vì không có lệnh ghi.
- `StudentController`: thêm `PUT /api/students/{id}/class/{classId}` gọi
  `transferClass`, trả `StudentResponse` (không trả entity).

### Thực nghiệm — tái hiện 2 trường hợp `@Transactional` không rollback

Chạy app thật (profile `dev`, DB Postgres dev qua `compose.yaml`), gọi API
bằng `curl`, kiểm tra dữ liệu bằng `psql` sau mỗi lần gọi. Code demo chỉ thêm
tạm vào `StudentService`/`StudentController` (2 method + 2 endpoint), chạy
xong xoá sạch — không còn trong code commit.

**Case 1 — ném checked exception sau khi đã `save` student:**

```java
@Transactional
public StudentResponse demoCheckedException(...) throws Exception {
    ...
    studentRepository.save(student);           // (1) update class_id
    throw new java.io.IOException("...");      // checked exception
    // (2) save history — không bao giờ chạy tới
}
```

- Gọi API chuyển sinh viên id=2 từ lớp 5 sang lớp 1 → response `500`.
- Kiểm tra DB: `students.class_id` của sinh viên 2 đã đổi thành `1`
  (**commit thật**, không rollback), `class_transfer_history` không có dòng
  mới (vì exception ném ra trước dòng `save` thứ 2, không có liên quan đến
  rollback).
- **Lý do**: quy tắc rollback mặc định của `@Transactional` chỉ áp dụng cho
  `RuntimeException`/`Error` (unchecked). Checked exception (`IOException`,
  hay bất kỳ exception kế thừa `Exception` không phải `RuntimeException`)
  **không** kích hoạt rollback trừ khi khai báo tường minh
  `@Transactional(rollbackFor = Exception.class)` (hoặc liệt kê đúng class
  exception đó). Đây là lý do (1) `update` bị commit dù hàm ném exception —
  `@Transactional` có mặt nhưng "không cứu" được vì sai loại exception.

**Case 2 — gọi method `@Transactional` qua self-invocation (`this.`):**

```java
public StudentResponse demoSelfInvocation(Long studentId, Long classId) {
    StudentResponse r = this.transferClass(studentId, classId); // gọi qua this
    throw new RuntimeException("...");                           // unchecked
}
```

- Gọi API chuyển sinh viên id=2 từ lớp 2 sang lớp 3 → response `500`
  (`RuntimeException` ném ra sau khi `transferClass` chạy xong).
- Kiểm tra DB: `students.class_id` đã đổi thành `3` **và** có dòng mới trong
  `class_transfer_history` (`from_class_id=2, to_class_id=3`) — cả 2 lệnh
  ghi đều **commit**, dù exception ném ra là unchecked (đáng lẽ phải
  rollback).
- **Lý do**: Spring triển khai `@Transactional` bằng AOP proxy (JDK dynamic
  proxy/CGLIB) — advice (mở/rollback transaction) chỉ chạy khi gọi method
  **từ bên ngoài proxy** (qua bean do Spring quản lý, vd Controller gọi
  `studentService.transferClass(...)`). Khi một method khác **trong cùng
  class** gọi `this.transferClass(...)`, lời gọi đi thẳng vào object gốc,
  bỏ qua proxy hoàn toàn → `@Transactional` trên `transferClass` không có
  tác dụng gì trong ngữ cảnh này. Vì không có transaction bao ngoài,
  `studentRepository.save()` và `classTransferHistoryRepository.save()` mỗi
  cái tự commit ngay trong transaction ngầm riêng của chính nó (Spring Data
  JPA tự bọc transaction cho từng method CRUD) — `throw` ở cuối không rollback
  được gì vì lúc đó cả 2 đã commit xong từ trước.

### Build & test

- `mvn -o compile`: sạch (cả lúc có code demo tạm và sau khi xoá).
- `mvn -o test` (toàn bộ suite, có Docker + DB dev chạy): **55/55 pass**,
  không regression. Chưa có test riêng cho `transferClass`/endpoint mới —
  để ở mục "người học tự làm".
- Gặp sự cố môi trường khi chạy thử nghiệm (không phải bug code): có 1 tiến
  trình `spring-boot:run` cũ còn sống từ trước (cổng 8081), và sau khi
  `taskkill /F` một tiến trình Java đang giữa chừng `CREATE TABLE` (migration
  V3), kết nối Postgres của nó không đóng sạch ngay (Postgres vẫn thấy
  session "idle in transaction" giữ lock bảng hệ thống) khiến lần khởi động
  kế tiếp bị treo ở bước Flyway migrate. Xử lý bằng
  `SELECT pg_terminate_backend(pid)` cho các session đó ở phía Postgres —
  không liên quan gì đến logic `@Transactional` của bài học, chỉ là tác dụng
  phụ của việc kill tiến trình Java thô bạo thay vì shutdown sạch.

## Phụ lục — sửa catch-all che mất 404 cho route không khớp

Phát hiện ở trên (URL không khớp route nào trả `500` thay vì `404`) được xử
lý luôn trong buổi này theo yêu cầu, thay vì để dành.

### Nguyên nhân (xác nhận qua log thật, không suy đoán)

Gọi `GET /api/khong-ton-tai`, log app bắt được:

```
ERROR ... c.t.s.exception.GlobalExceptionHandler   : Unexpected error
org.springframework.web.servlet.resource.NoResourceFoundException:
No static resource api/khong-ton-tai for request '/api/khong-ton-tai'.
```

Khi không có `@RequestMapping` nào khớp, Spring MVC fallback thử tìm resource
tĩnh (`static/`), không thấy thì ném `NoResourceFoundException` — Spring có
sẵn xử lý mặc định cho exception này trả về `404`. Nhưng
`@ExceptionHandler(Exception.class)` (catch-all) trong `GlobalExceptionHandler`
bắt luôn exception này trước khi Spring's default resolver kịp chạy, nên mọi
route không khớp đều rơi vào nhánh "lỗi không xác định" → `500`. Đúng pattern
đã ghi trong CLAUDE.md: không dựa vào catch-all `Exception.class` vì che mất
behavior mặc định Spring tự xử lý đúng cho một số exception cụ thể.

### Sửa (AI viết, theo yêu cầu)

`GlobalExceptionHandler`: thêm 1 handler riêng, đặt trước catch-all, theo
đúng pattern các handler khác (`NotFoundException` → 404):

```java
@ExceptionHandler(NoResourceFoundException.class)
public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Không tìm thấy endpoint");
}
```

### Build & test

- `mvn -o compile`: sạch.
- Verify lại bằng app thật: `GET /api/khong-ton-tai` → `404` (trước đó
  `500`); `GET /api/students/1` (route hợp lệ) và `GET /api/students/999999`
  (`NotFoundException` nghiệp vụ) vẫn đúng như cũ — không ảnh hưởng 2 case
  này.
- `mvn -o test` (toàn bộ suite, có Docker + DB dev chạy): **55/55 pass**,
  không regression.

## Người học tự làm

- Tự tái hiện lại Case 1 (checked exception): thêm `rollbackFor =
  Exception.class` vào `@Transactional` của một method ném checked
  exception, gọi lại, xác nhận lần này `class_id` **không** đổi (rollback
  đúng) — để thấy rõ khác biệt so với kết quả ở trên.
- Tự tái hiện lại Case 2 (self-invocation) với 1 thay đổi: gọi
  `transferClass` từ **Controller** (qua bean proxy, như endpoint chính
  thức đang làm) thay vì tự gọi `this.` trong Service, xác nhận
  `@Transactional` hoạt động bình thường — hiểu rằng vấn đề chỉ xảy ra khi
  tự gọi method cùng class, không phải lỗi của bản thân `@Transactional`.
- Viết thêm test cho `PUT /api/students/{id}/class/{classId}` (happy path
  200, 404 khi sai `id`/`classId`) và cho `NoResourceFoundException` (404
  khi gọi route không tồn tại) — `StudentControllerTest` hiện chưa có.
