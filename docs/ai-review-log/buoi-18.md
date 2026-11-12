# Buổi 18 — Thứ Sáu 27/11/2026 — Test repository với @DataJpaTest

Yêu cầu: `@DataJpaTest` cho `StudentRepository` và `EnrollmentRepository` (query
method, `@Query`, đăng ký trùng phải ném exception). Hỏi lại trước khi code vì
đây là quyết định ảnh hưởng `pom.xml` (phạm vi `CLAUDE.md` yêu cầu không tự ý
thêm dependency) — người học chọn **Testcontainers PostgreSQL** thay vì trỏ
vào Postgres Docker đang chạy (`@AutoConfigureTestDatabase(replace = NONE)` +
container `student-api-postgres` có sẵn).

## Thêm dependency vào `pom.xml` (theo yêu cầu, có review)

- `spring-boot-starter-data-jpa-test`, `spring-boot-testcontainers`,
  `org.testcontainers:testcontainers-junit-jupiter`,
  `org.testcontainers:testcontainers-postgresql` — tất cả `scope=test`,
  không ghi version tường minh (quản lý qua `spring-boot-dependencies` BOM
  của Spring Boot 4.1.2, kéo theo `testcontainers-bom:2.0.5`).
- **Phát hiện khi build thử**: Testcontainers 2.0 đổi tên artifact —
  `org.testcontainers:junit-jupiter` / `org.testcontainers:postgresql` (tên
  cũ, quen thuộc ở Testcontainers 1.x) không còn tồn tại trong BOM 2.0.5, phải
  là `testcontainers-junit-jupiter` / `testcontainers-postgresql` (có tiền tố
  `testcontainers-`). Cũng đổi API: `PostgreSQLContainer` ở package mới
  `org.testcontainers.postgresql` **không còn generic** (`PostgreSQLContainer`
  thay vì `PostgreSQLContainer<?>` kiểu self-type cũ) — package cũ
  `org.testcontainers.containers.PostgreSQLContainer` vẫn còn (tương thích
  ngược) nhưng dùng thẳng package mới cho đúng version đang quản lý.
- Import `@DataJpaTest` / `@AutoConfigureTestDatabase` cũng đổi package ở
  Spring Boot 4 so với quán tính Spring Boot 3: `org.springframework.boot.data
  .jpa.test.autoconfigure.DataJpaTest`,
  `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`
  (không còn `org.springframework.boot.test.autoconfigure.orm.jpa.*`).

## Test (AI viết, theo yêu cầu)

- `repository/StudentRepositoryTest.java` (5 test): `findByFullNameContainingIgnoreCase`
  (khớp 1 phần + không phân biệt hoa thường, và trường hợp không khớp gì),
  `findByBirthDateBetween` (bao gồm cả biên), `findBySchoolClassId` (chỉ lấy
  đúng lớp, có lớp khác xen vào để chắc không bị lẫn), `findStudentsWithoutCourse`
  (có sinh viên đã đăng ký lẫn chưa đăng ký, kiểm tra tách đúng).
- `repository/EnrollmentRepositoryTest.java` (3 test): `existsByStudentIdAndCourseId`
  (false lúc chưa đăng ký, true sau khi đăng ký), và **đăng ký trùng**:
  `saveAndFlush` enrollment thứ 2 cùng `(student_id, course_id)` phải ném
  `DataIntegrityViolationException` — test thẳng ràng buộc `unique
  (student_id, course_id)` ở tầng DB thật (constraint `uk_enrollment_student_course`
  trong `Enrollment` entity), **không** test lại logic `existsByStudentIdAndCourseId`
  của `EnrollmentService` (đã có ở `EnrollmentControllerTest` bằng mock) — mục
  đích là bảo vệ race condition nếu 2 request chạy đồng thời đều vượt qua check
  `exists` trước khi insert.
- Mỗi test class tự khởi `@Container static PostgreSQLContainer postgres` +
  `@ServiceConnection` riêng (không dùng chung 1 container qua base class) —
  đơn giản hơn, đổi lại mỗi test class tốn ~5-12s khởi container riêng.
- `@AutoConfigureTestDatabase(replace = Replace.NONE)`: bắt buộc, vì
  `@DataJpaTest` mặc định tự thay datasource bằng embedded DB (H2) — nếu
  không có dòng này, `@ServiceConnection` trỏ vào Testcontainers Postgres bị
  bỏ qua.
- `@DynamicPropertySource` ép `spring.jpa.hibernate.ddl-auto=create-drop`:
  container Testcontainers luôn là DB rỗng mới tinh, không giống Postgres dev
  (`ddl-auto: update`) đã có schema sẵn — phải tự tạo bảng từ entity Java mỗi
  lần chạy test rồi xóa khi xong, không ảnh hưởng schema dev thật.

## Build & test

- `mvn -o compile`: lỗi thiếu version 2 dependency Testcontainers (do nhầm tên
  artifact cũ) → sửa tên artifact, build lại sạch.
- `mvn -o test-compile`: lỗi tiếp `PostgreSQLContainer does not take
  parameters` (do dùng generic `<?>` với class không còn generic ở package
  mới) → bỏ `<?>`, sửa thành `PostgreSQLContainer postgres = new
  PostgreSQLContainer(...)`.
- `mvn -o test -Dtest=StudentRepositoryTest,EnrollmentRepositoryTest`: **8/8
  pass**, `BUILD SUCCESS` (~90s, phần lớn thời gian là khởi động 2 container
  Postgres + kéo image `postgres:16-alpine` lần đầu + Ryuk reaper). Log xác
  nhận đúng hành vi mong đợi: `ERROR: duplicate key value violates unique
  constraint "uk_enrollment_student_course"` khi insert enrollment trùng.
- `mvn -o test` (toàn bộ suite): **BUILD SUCCESS**, không có regression ở các
  test có sẵn (`@WebMvcTest` của 4 controller, `StudentApiApplicationTests`).

## Người học tự làm

- Lần đầu chạy `mvn test` với Testcontainers sẽ kéo image `postgres:16-alpine`
  và `testcontainers/ryuk` — cần mạng ổn định, lần sau sẽ nhanh hơn vì đã cache
  local. Docker Desktop phải đang chạy, nếu không `@Testcontainers` sẽ fail
  ngay ở bước tạo container (không phải lỗi code).
- Đối chiếu lại 2 file test, hiểu rõ từng case trước khi merge — đặc biệt case
  "đăng ký trùng" test thẳng DB constraint chứ không phải gọi qua `EnrollmentService`
  (khác với `EnrollmentControllerTest` đã có từ buổi trước, test đó mock
  service nên không chạm DB thật).
- Cân nhắc có cần gộp phần khởi `@Container`/`@ServiceConnection` lặp lại ở 2
  test class thành 1 class cha dùng chung không (hiện đang để riêng cho đơn
  giản, đổi lại tốn thời gian chạy test hơn) — chưa làm vì không thuộc yêu cầu
  buổi này, để tự quyết nếu thấy cần.
