# Buổi 3 — Project Spring Web đầu tiên

- Ngày học: 06/11/2026
- Thời gian thực tế:

## Phần 1 — Spring: project `student-api`

- Tạo project trên start.spring.io: Maven / Java, Group `com.thienlv`, Artifact `student-api`, Packaging Jar, Java 21, dependency duy nhất **Spring Web**.
- Đổi `application.properties` → `application.yml` (properties dùng dấu `.` phân cấp, YAML dùng thụt lề để phân cấp); đổi `server.port` sang `8081`.
- Ghi chú lỗi gặp phải khi setup: IntelliJ báo `Cannot compile module 'student-api' configured for JVM target 5` — do Maven chưa import xong `pom.xml` nên IntelliJ dùng JVM target mặc định/fallback sai; fix bằng Reload Maven project (và kiểm tra lại Project Structure > SDK / Language level nếu reload không đủ).

### 4 endpoint GET đã viết

| Endpoint | Annotation chính | Mục đích |
|---|---|---|
| `GET /hello?name=` | `@RestController`, `@GetMapping`, `@RequestParam(required=false)` | Làm quen request/response đơn giản |
| `GET /api/students` | `@GetMapping`, trả `List<Student>` | Danh sách cứng (record `Student`, field `static final List`) |
| `GET /api/students/{id}` | `@PathVariable` | Lấy 1 sinh viên theo id, dùng `Stream.filter().findFirst().orElseThrow()` |
| `GET /api/students?name=&minAge=&maxAge=` | nhiều `@RequestParam(required=false)` | Lọc theo tên + khoảng tuổi (tính từ `birth_date` bằng `Period.between`) |

### Tự trả lời

- Vì sao không cần khai báo `HelloController`/`StudentController` trong `main`?
  - `StudentApiApplication` có `@SpringBootApplication` (bao gồm `@ComponentScan`) → Spring tự quét các package con, tìm class có `@RestController`/`@Service`/... rồi tự tạo bean. Đây là phần Spring "làm hộ" thay cho việc tự `new` + lắp ráp object trong `main` ở bài DI thủ công buổi 2.
- `@PathVariable` khác `@RequestParam` ở đâu?
  - `@PathVariable` lấy giá trị từ chính đường dẫn URL (`/api/students/{id}` → `id` nằm trong path). `@RequestParam` lấy từ query string sau dấu `?` (`?name=...&minAge=...`).
- Vì sao `Optional<Student>` (từ `findFirst()`) không trả thẳng được khi method khai báo kiểu trả về là `Student`?
  - `Optional` là một "hộp" có thể rỗng hoặc có giá trị — không phải chính kiểu `Student`. Cần `.orElse(...)` hoặc `.orElseThrow()` để lấy giá trị thật ra (hoặc xử lý trường hợp rỗng).
- Lỗi logic đã gặp khi lọc theo `name`: dùng `Objects.equals(student.full_name(), name)` khi `name` là `null` (không truyền query param) sẽ loại bỏ **toàn bộ** sinh viên, vì không ai có tên `null`. Cách sửa: thêm điều kiện `name == null || ...` giống cách đã làm đúng với `minAge`/`maxAge`.

## Phần 2 — DB: ràng buộc dữ liệu

File: `sql/week-3/03-constraints.sql`

### Giải thích ràng buộc

- **`PRIMARY KEY`**: khóa chính, định danh duy nhất mỗi dòng; tự bao gồm `NOT NULL` + `UNIQUE`.
- **`NOT NULL`**: cấm để trống giá trị ở cột đó khi insert.
- **`UNIQUE`**: không cho 2 dòng trùng giá trị ở cột đó (áp cho `email`).
- **`CHECK (điều_kiện)`**: ràng buộc một điều kiện logic phải đúng cho mọi dòng, ví dụ `CHECK (birth_date <= CURRENT_DATE)` — ngăn ngày sinh ở tương lai.
- **`DEFAULT giá_trị`**: nếu insert không chỉ định giá trị cho cột đó, Postgres tự điền giá trị mặc định (vd `DEFAULT CURRENT_TIMESTAMP` cho `created_at`).
- **`GENERATED ALWAYS AS IDENTITY`**: cách chuẩn để tạo cột tự tăng (thay cho `SERIAL` cũ); không được tự gán giá trị cho cột này khi insert.

### Câu lệnh tạo bảng

```sql
CREATE TABLE students (
  id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  birth_date DATE CHECK (birth_date <= CURRENT_DATE),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 5 kiểu INSERT sai — chép lại thông báo lỗi thật

| # | Vi phạm ràng buộc | SQL state | Thông báo lỗi chính |
|---|---|---|---|
| 1 | `NOT NULL` (thiếu `full_name`) | 23502 | `null value in column "full_name" ... violates not-null constraint` |
| 2 | `UNIQUE` (trùng `email`) | 23505 | `duplicate key value violates unique constraint "students_email_key"` |
| 3 | `CHECK` (ngày sinh tương lai) | 23514 | `violates check constraint "students_birth_date_check"` |
| 4 | Tự gán `id` cho cột `GENERATED ALWAYS` | 428C9 | `cannot insert a non-DEFAULT value into column "id"` (gợi ý dùng `OVERRIDING SYSTEM VALUE`) |
| 5 | Sai kiểu dữ liệu (chữ vào cột `DATE`) | 22007 | `invalid input syntax for type date: "..."` |

Nhận xét: 3 case đầu (`23xxx`) là lỗi **vi phạm ràng buộc** (constraint violation) — dữ liệu đúng kiểu nhưng sai quy tắc. Case 4 (`428C9`) là Postgres từ chối ngay ở bước phân tích câu lệnh vì cố ghi đè cột identity. Case 5 (`22007`) là lỗi sớm nhất — sai ngay từ bước convert kiểu dữ liệu, chưa kịp tới bước kiểm tra ràng buộc.

## Vướng mắc / câu hỏi khi làm

-

## Nhật ký lỗi AI gặp phải (nếu có hỏi AI giải thích lỗi)

| Lỗi/thông báo | AI giải thích gì | Mình tự rút ra điều gì |
|---------------|-------------------|--------------------------|
|               |                   |                          |
