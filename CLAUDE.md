# CLAUDE.md — Quy ước cho AI khi làm việc trên dự án này

Dự án học tập cá nhân (self-study) về Spring Boot. File này là "rules" cho AI
(Claude Code, Cursor, ...) khi được yêu cầu đọc/sửa/viết code trong repo.

## Tech stack

- Java + Spring Boot 4.1.2 (Spring Framework/Boot 4 — **không** còn dùng
  `com.fasterxml.jackson.databind` (đổi sang Jackson 3, package `tools.jackson.*`)
  và `@MockBean` (đã bị gỡ, dùng `@MockitoBean`). Luôn kiểm tra lại API theo
  đúng version 4, không suy đoán theo quán tính Spring Boot 3.
- Module chính: `student-api/` (package gốc `com.thienlv.student_api`).
- **Chưa dùng Spring Data JPA/DB thật trong app** — `pom.xml` hiện chỉ có
  `spring-boot-starter-webmvc` + `validation`. Tầng `repository/` là
  **in-memory, lưu bằng `Map`** (xem `StudentRepository`: `LinkedHashMap`
  + `AtomicLong` sinh id), không phải `JpaRepository`. Schema SQL ở `sql/`
  là track học riêng ("DB song song"), chưa nối với app — không coi đó là
  nguồn sự thật cho code Java trừ khi được yêu cầu nối DB thật.
- Lombok + MapStruct cho mapping entity ↔ DTO.

## Kiến trúc 3 tầng

Luôn giữ đúng luồng: **Controller → Service → Repository → Database**.

- `controller/`: chỉ nhận request, validate (`@Valid`), gọi Service, trả response.
  Không chứa business logic, không query trực tiếp.
- `service/`: chứa toàn bộ business logic (kiểm tra trùng email, tính tuổi,
  ném exception nghiệp vụ...). Không trả về Entity ra ngoài tầng Controller —
  luôn map sang DTO response trước khi return.
- `repository/`: chỉ thao tác dữ liệu (hiện là in-memory `Map`, theo đúng
  pattern của `StudentRepository`). Không chứa business logic.
- `entity/`: ánh xạ bảng DB.
- `dto/`: Request/Response record, có thể gắn validation annotation.
- `mapper/`: MapStruct interface, `@Mapper(componentModel = "spring")`.
- `exception/`: custom exception + `GlobalExceptionHandler` (`@RestControllerAdvice`).

Không gộp logic giữa các tầng. Ví dụ: không query DB trong Controller, không
build ResponseEntity trong Service.

## Cách đặt tên

- Package: snake_case theo tên project đã có sẵn (`com.thienlv.student_api`),
  nhưng **tên class/interface/method/field: chuẩn Java** — `PascalCase` cho
  class, `camelCase` cho method/field/biến. Không tạo field kiểu `full_name`
  trong Java trừ khi đang map tới cột DB hiện có (entity hiện tại dùng tên
  field theo cột DB — giữ nguyên, không đổi tuỳ tiện).
- DTO request/response: hậu tố rõ ràng `XxxRequest`, `XxxResponse`
  (vd: `StudentRequest`, `StudentResponse`).
- Exception nghiệp vụ: hậu tố `Exception`, đặt trong `exception/`
  (vd: `NotFoundException`, `DuplicateEmailException`).
- Mapper: hậu tố `Mapper` (vd: `StudentMapper`).
- Tên REST endpoint: số nhiều, kebab/lowercase, có tiền tố `/api`
  (vd: `/api/students`, `/api/students/{id}`).
- Tránh đặt tên entity/class trùng tên class nền tảng của Java (`Class`,
  `Object`, `String`, `Thread`...) dù không lỗi compile, vì `java.lang.*`
  tự động import và sẽ bị class của mình che mất, gây nhầm lẫn khi đọc code.
  Vd: entity cho "lớp học" đặt tên `SchoolClass`, không đặt `Class`.

## DTO dạng record

- DTO (request/response) **luôn là `record`**, không dùng class thường có
  getter/setter thủ công.
- Validation annotation (`@NotBlank`, `@Size`, `@Email`, `@Past`...) gắn trực
  tiếp lên từng component của record request.
- Không thêm logic vào DTO ngoài việc giữ dữ liệu (nếu cần tính toán như
  `age`, map ở tầng `mapper/`, không nhét vào record).

## Constructor injection

- Luôn dùng **constructor injection** cho dependency (`StudentService`,
  `StudentRepository`, `StudentMapper`...). Không dùng `@Autowired` trên field.
- Field dependency khai báo `private final`.
- Nếu class có đúng 1 constructor, không bắt buộc phải thêm `@Autowired`
  (Spring tự nhận constructor duy nhất).
- Nếu dùng Lombok để giảm boilerplate constructor, dùng `@RequiredArgsConstructor`
  trên các field `private final` — không viết constructor thủ công song song
  với Lombok.

## Format lỗi: ProblemDetail

Toàn bộ lỗi trả về client đi qua `GlobalExceptionHandler`
(`@RestControllerAdvice`), dùng `org.springframework.http.ProblemDetail`:

- `NotFoundException` → `404 NOT_FOUND`.
- `DuplicateEmailException` → `409 CONFLICT`.
- `MethodArgumentNotValidException` (lỗi `@Valid`) → `400 BAD_REQUEST`, kèm
  property `errors` là map `field -> message` (dùng
  `ProblemDetail#setProperty("errors", ...)`).
- Mọi exception khác → `500 INTERNAL_SERVER_ERROR`, có `log.error(...)` trước
  khi trả response, **không** để lộ message/stacktrace gốc ra ngoài.
- Khi thêm case lỗi mới (vd: `HttpMessageNotReadableException` cho JSON sai
  định dạng), thêm handler riêng cho đúng exception đó — không dựa vào
  catch-all `Exception.class`, vì catch-all đang che mất behavior mặc định
  400 mà Spring tự xử lý cho một số exception.

## Test bằng @WebMvcTest

- Test Controller dùng `@WebMvcTest(XxxController.class)` + `MockMvc`, mock
  tầng Service bằng `@MockitoBean` (không dùng `@MockBean` — đã bị gỡ ở
  Spring Boot 4).
- Import đúng theo Spring Boot 4 / Jackson 3:
  - `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`
  - `tools.jackson.databind.ObjectMapper` (không phải
    `com.fasterxml.jackson.databind.ObjectMapper`).
- Mỗi test case nên cover: happy path (200/201/204), not-found (404),
  validation lỗi theo từng field (400 kèm đúng field trong `errors`),
  trùng dữ liệu (409) nếu có.
- Trước khi AI viết test, tự liệt kê checklist case cần test, đối chiếu với
  case AI đề xuất thêm — không chấp nhận mù quáng, phải hiểu từng test trước
  khi merge.
- Nếu test phát hiện bug có sẵn trong code (không phải do AI viết sai), ghi
  rõ trong `docs/ai-review-log.md`: test đó đang ghi nhận hành vi hiện tại
  (có thể là bug), không phải hành vi mong muốn cuối cùng.

## Quy tắc chung khi AI hỗ trợ (self-study)

- Không tự ý thêm dependency vào `pom.xml` nếu không được yêu cầu rõ ràng.
- Không đổi schema DB nếu không được yêu cầu.
- Giữ thay đổi tối thiểu, đúng phạm vi được giao — không refactor lan man.
- Sau khi AI sửa/viết code, luôn ghi lại vào `docs/ai-review-log.md`: AI làm
  gì, không làm gì, người học tự làm gì — theo đúng format các buổi trước.
- Không thêm `Co-Authored-By: Claude` vào commit message.
