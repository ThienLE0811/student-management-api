# Buổi 19 — Thứ Hai 30/11/2026 — Phân trang và Flyway

Phần 1 (40 phút): `GET /api/students?page=&size=&sort=` dùng `Pageable`; map
`Page<Student>` sang DTO trước khi trả (không trả entity); giới hạn size tối
đa.

## Quyết định cần hỏi trước khi code

`GET /api/students` hiện tại (từ buổi trước) đã lọc theo `name`/`minAge`/`maxAge`
bằng Stream trong `StudentService.getAll`, dùng `findAll()` tải toàn bộ bảng
rồi lọc trong memory — `minAge`/`maxAge` tính theo tuổi suy ra từ `birth_date`
(computed field), không phải cột DB, nên không thể đẩy thẳng xuống
`JpaRepository` bằng derived query/`@Query` đơn giản.

Hỏi người học: giữ filter cũ (áp phân trang lên **kết quả đã lọc**, tự cắt
trang trong memory) hay bỏ filter để làm `Pageable` thuần qua
`findAll(Pageable)`. Người học chọn **giữ nguyên filter, thêm phân trang cho
kết quả đã lọc** — chấp nhận đây không phải phân trang ở tầng DB thật (không
tiết kiệm được việc tải toàn bộ bảng), vì ưu tiên không phá filter đã có.

## Code (AI viết, theo yêu cầu)

- `StudentService.getAll(name, minAge, maxAge, Pageable)`: giữ nguyên 2 bước
  filter Stream cũ, sau đó:
  - `limitPageSize`: nếu `pageable.getPageSize() > 100` thì tạo lại
    `PageRequest` với size ép về 100 (`MAX_PAGE_SIZE`) — business rule nên đặt
    ở Service, không ở Controller.
  - `sortInMemory`: áp `Sort` của `Pageable` lên list đã lọc bằng
    `Comparator` dựng qua `BeanWrapperImpl` (đọc property theo tên field,
    vd: `sort=full_name,desc` khớp đúng tên field entity hiện tại — không
    phải tên theo chuẩn Java camelCase vì entity đang dùng tên cột DB).
  - Cắt trang bằng `subList(fromIndex, toIndex)` theo `offset`/`pageSize` của
    `Pageable` đã giới hạn, rồi bọc `PageImpl<>(pageContent, pageable, total)`
    và `.map(studentMapper::toResponse)` ra `Page<StudentResponse>` — không
    trả `Page<Student>` (entity) ra ngoài Controller.
- `StudentController.getListStudent`: thêm tham số
  `@PageableDefault(size = 20, sort = "id") Pageable pageable`, forward
  nguyên xuống Service (không tự giới hạn size ở đây — Controller không chứa
  business logic).
- Không đổi API path/method, không đổi `pom.xml`, không đổi schema DB.

## Test (AI sửa lại test cũ, theo yêu cầu)

- 2 test cũ của `getListStudent` trong `StudentControllerTest` phải sửa vì đổi
  kiểu trả về `List<StudentResponse>` → `Page<StudentResponse>`: mock trả
  `PageImpl<>(...)`, assert qua `$.content[...]` thay vì `$[...]` (Spring Data
  Jackson module bọc `Page` thành `{"content": [...], "page": {...}}`).
- **Không thêm** test "giới hạn size tối đa" ở tầng Controller: logic clamp
  nằm trong `StudentService` (đã mock ở `@WebMvcTest`), nên 1 test Controller
  gọi `?size=1000` sẽ không chạm được code clamp thật — AI có viết thử rồi tự
  bỏ vì nó không verify được gì ngoài những gì 2 test kia đã check. Logic
  `limitPageSize`/`sortInMemory` hiện **chưa có unit test riêng** (dự án chưa
  có `StudentServiceTest`) — để người học tự quyết có cần thêm ở buổi sau
  không.

## Build & test

- `mvn -o compile`: sạch.
- `mvn -o test -Dtest=StudentControllerTest`: **17/17 pass**.
- `mvn -o test` (toàn bộ suite): 3 lỗi — `StudentApiApplicationTests`
  (không có DB `dev` chạy), `StudentRepositoryTest`/`EnrollmentRepositoryTest`
  (Testcontainers không kéo được Docker image lúc chạy, không có Docker
  Desktop đang mở). Cả 3 đều là môi trường (không có DB/Docker lúc chạy lệnh),
  không liên quan thay đổi lần này — `StudentControllerTest` và các
  `@WebMvcTest` khác vẫn pass, không có regression.

## Người học tự làm

- Chạy lại `mvn test` khi có Docker Desktop mở (cho Testcontainers) hoặc DB
  dev chạy (cho `contextLoads`) để xác nhận lại toàn bộ suite xanh.
- Đối chiếu kỹ `sortInMemory` (dùng `BeanWrapperImpl` đọc property theo tên
  field Java, vd `full_name`) — hiểu rõ vì sao `sort=fullName` (camelCase)
  sẽ ném lỗi "property không tồn tại" thay vì im lặng bỏ qua, còn
  `sort=full_name` mới khớp tên field thật.
- Lưu ý: cách làm này paginate trên list đã tải toàn bộ (`findAll()`) và lọc
  trong memory — không phải phân trang thật ở tầng DB. Nếu dữ liệu lớn, đây
  là điểm cần tối ưu lại sau (có thể dùng `Specification`/`@Query` để đẩy
  filter + phân trang xuống DB), nhưng ngoài phạm vi 40 phút đầu buổi này.

---

## Phần 2 (55 phút) — Thêm Flyway

Yêu cầu: chỉnh `schema.sql` cho khớp entity rồi đặt thành
`db/migration/V1__init.sql`; đổi `ddl-auto` sang `validate`; xóa database cũ,
chạy lại từ đầu; thêm `V2__add_indexes.sql` cho 2 index đã tạo ở tuần 5
(buổi 15, 16).

### Thêm dependency vào `pom.xml` (theo yêu cầu, có review)

- **Phát hiện khi build thử**: Spring Boot 4 tách Flyway thành module riêng —
  không còn tự động kích hoạt chỉ bằng cách thêm `org.flywaydb:flyway-core`
  như quán tính Spring Boot 3 (`spring-boot-autoconfigure-4.1.2.jar` không
  còn chứa `FlywayAutoConfiguration`). Phải dùng
  `org.springframework.boot:spring-boot-starter-flyway` (kéo theo module
  `spring-boot-flyway` chứa autoconfiguration + `flyway-core` managed version
  qua BOM) — cùng pattern tách module đã gặp ở buổi 18
  (`spring-boot-data-jpa-test`, `spring-boot-jdbc-test`...).
- Thêm riêng `org.flywaydb:flyway-database-postgresql` (`scope=runtime`):
  `flyway-core` từ bản 10 trở lên tách hỗ trợ từng DB ra module riêng, không
  tự nhận diện được Postgres nếu thiếu dependency này — không ghi version
  tường minh, quản lý qua BOM.
- Không đụng vào dependency JPA/Postgres driver đã có.

### `db/migration/V1__init.sql` (AI viết, đối chiếu entity, người học review)

- Đối chiếu từng entity hiện có (`Student`, `SchoolClass`, `Course`,
  `Enrollment`) thay vì dùng lại `sql/week-3/06-schema.sql` — file đó chỉ là
  track học riêng ("DB song song"), thiếu cột `created_at`/`class_id` đã thêm
  từ buổi 14-15 và thiếu bảng `classes`.
- 4 bảng theo đúng thứ tự phụ thuộc FK: `classes`, `courses` (không phụ
  thuộc) → `students` (FK `class_id`) → `enrollments` (FK `student_id`,
  `course_id`).
- Kiểu cột khớp đúng mapping mặc định của Hibernate khi không khai báo
  `@Column(length=...)`: `VARCHAR(255)` cho các field `String`
  (`full_name`, `email`, `name`...), `DOUBLE PRECISION` cho `Enrollment.score`
  (field `Double` — **không** phải `NUMERIC` như `sql/week-3/06-schema.sql`
  cũ, dễ nhầm vì bản cũ dùng `NUMERIC`).
- Giữ đúng tên constraint đã khai báo tường minh trong entity:
  `uk_enrollment_student_course` (đúng `name` trong
  `@UniqueConstraint` của `Enrollment`). Các unique khác (`classes.name`,
  `courses.name`, `students.email`) không có tên tường minh trong entity nên
  tự đặt tên rõ nghĩa (`uk_classes_name`, `uk_courses_name`,
  `uk_students_email`) — không bắt buộc khớp hash Hibernate tự sinh trước đây,
  vì `ddl-auto=validate` không kiểm tra tên constraint.
- **Không** gồm 2 index hiệu năng (`idx_students_class_id`,
  `idx_enrollments_course_id_student_id`) trong V1 — tách riêng sang V2 theo
  đúng yêu cầu đề bài.

### `db/migration/V2__add_indexes.sql`

- Đúng 2 lệnh `CREATE INDEX` đã chạy tay ở buổi 15/16
  (`sql/week-5/12-index-fk.sql`, `sql/week-5/13-enrollments-multicolumn-index.sql`),
  kèm lại lý do ngắn gọn (FK không tự có index; `course_id` phải đứng trước
  trong index nhiều cột theo leftmost prefix rule) để migration tự giải thích
  được khi đọc lại sau này.

### Đổi `ddl-auto` và chạy lại từ đầu

- `application-dev.yml`: `ddl-auto: update` → `ddl-auto: validate`.
- Hỏi người học trước khi đụng container Postgres dev (thao tác phá hủy dữ
  liệu) — chọn `docker compose down -v` rồi `up -d` lại (xóa hẳn volume, tạo
  container/volume mới hoàn toàn) thay vì chỉ `DROP SCHEMA public CASCADE`.
- Lần chạy thử đầu tiên thất bại: `Schema validation: missing table
  [classes]` — vì khi đó `pom.xml` mới có `flyway-core` (chưa có
  `spring-boot-starter-flyway`), Flyway không tự kích hoạt nên không migrate
  trước khi Hibernate validate. Sửa dependency xong, chạy lại:
  Flyway log rõ `Migrating schema "public" to version "1 - init"` →
  `"2 - add indexes"` → `Successfully applied 2 migrations`, sau đó Hibernate
  `validate` pass, app start thành công (profile `dev`).
- Xác nhận lại bằng `psql \d students`, `\d enrollments`: đúng cột, đúng FK,
  đúng unique constraint, đúng 2 index tuần 5; bảng `flyway_schema_history`
  ghi nhận cả V1 và V2 đã apply thành công.

### Build & test

- `mvn -o compile`: sạch sau khi sửa dependency.
- Chạy thử `spring-boot:run -Dspring-boot.run.profiles=dev` 2 lần (lần đầu
  dính lỗi port 8081 còn bị chiếm bởi tiến trình java cũ từ lần chạy thất bại
  trước — `taskkill` xong chạy lại sạch).
- `mvn -o test` (toàn bộ suite, có Docker + DB dev đang chạy):
  **55/55 pass**, bao gồm cả `StudentApiApplicationTests` (cần DB dev thật) và
  `StudentRepositoryTest`/`EnrollmentRepositoryTest` (Testcontainers, cần
  Docker) — khác với lần chạy ở Phần 1 (3 lỗi do thiếu Docker/DB lúc đó).

### Người học tự làm

- Đọc lại `V1__init.sql`, đối chiếu từng cột/FK/unique với entity Java tương
  ứng — xác nhận hiểu đúng lý do chọn `DOUBLE PRECISION` cho `score` và vì
  sao tên constraint `uk_enrollment_student_course` phải khớp chính xác
  (entity khai báo tường minh) trong khi 3 unique còn lại thì không bắt buộc.
- Hiểu rõ thứ tự chạy: Flyway migrate trước → Hibernate `validate` sau — nếu
  sau này thêm cột/bảng mới vào entity mà quên viết migration tương ứng
  (`V3__...sql`), app sẽ fail ngay ở bước `validate` với lỗi tương tự
  "missing table/column" đã gặp ở lần chạy thử đầu.
- Từ nay mỗi lần đổi entity (thêm cột, đổi kiểu, thêm bảng...) phải tự viết
  thêm file `Vn__...sql` tương ứng — `ddl-auto: validate` sẽ không còn tự
  sinh/sửa schema giúp như `update` trước đây.
