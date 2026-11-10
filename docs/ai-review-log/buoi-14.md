# Buổi 14 — Thứ Hai 23/11/2026 — PostgreSQL + Spring Data JPA (setup)

- AI viết `compose.yaml` ở root (chạy container `postgres:16-alpine`, database `student_api`,
  user/password `student_api`/`student_api`, map port host `5433` → container `5432` để không đụng
  port của `student_db` dùng tay cho track SQL).
- AI thêm `spring-boot-starter-data-jpa` và `org.postgresql:postgresql` (runtime) vào
  `student-api/pom.xml` — không đụng dependency nào khác.
- AI thêm `spring.datasource.*` và `spring.jpa.*` cơ bản (`ddl-auto: validate`, `show-sql: true`)
  vào `application-dev.yml` — chưa đụng `application.yml` gốc hay `application-prod.yml`.
- AI **không** tạo JPA entity/repository thật, không đổi tầng `repository/` in-memory hiện có
  (`StudentRepository`, `ClassRepository` vẫn dùng `Map`) — việc nối entity/repository với DB thật
  để ở bước sau.
- Tự làm: chạy `docker compose up -d` để lên container Postgres, kiểm tra kết nối
  (`docker exec` vào `psql` hoặc dùng client), và tự hiểu từng dòng cấu hình datasource trước khi
  dùng tiếp (vì sao `ddl-auto: validate` chứ không phải `update`, vì sao driver là `runtime` scope).

## Cập nhật cùng buổi — nối Student với Postgres thật

- `entity/Student.java`: đổi từ `record` sang `@Entity` class (Lombok `@Getter/@Setter/
  @NoArgsConstructor/@AllArgsConstructor`), `@Id @GeneratedValue(strategy = IDENTITY)`,
  `@Column` map tới đúng tên cột cũ (`full_name`, `birth_date`, `created_at`), thêm
  `nullable = false` và `unique = true` trên `email` (codify lại đúng rule nghiệp vụ
  `DuplicateEmailException` đã có sẵn ở Service, không phải quy tắc mới).
- `repository/StudentRepository.java`: từ class tự quản `Map` → `interface extends
  JpaRepository<Student, Long>`.
- `service/StudentService.java`: đổi các lời gọi kiểu accessor của record (`student.id()`,
  `student.full_name()`...) sang getter (`student.getId()`, `student.getFull_name()`...);
  `deleteById` phải đổi logic vì `JpaRepository.deleteById` không trả `boolean` như bản Map
  cũ (ném `EmptyResultDataAccessException` nếu không tồn tại) → đổi thành `existsById` trước,
  không tồn tại thì `NotFoundException`, tồn tại thì mới `deleteById`.
- `mapper/StudentMapper.java`: chỉ sửa 1 dòng `student.birth_date()` →
  `student.getBirth_date()`; các `@Mapping(source = "full_name"/...)` giữ nguyên vì MapStruct
  tự resolve theo JavaBean property convention (`getFull_name()` → property `full_name`).
- `application-dev.yml`: `ddl-auto` đổi từ `validate` → `update` (tạm thời, đúng yêu cầu buổi
  này — tuần 6 sẽ thay bằng Flyway).

## Regression AI phát hiện và tự sửa — `StudentApiApplicationTests` fail sau khi thêm JPA

Sau khi thêm `spring-boot-starter-data-jpa`, test `@SpringBootTest` full context
(`StudentApiApplicationTests.contextLoads`) fail: context không active profile nào, mà
`application.yml` gốc (không profile) không khai báo `spring.datasource.*` — Spring Boot cố
tạo Hikari `DataSource` nhưng không biết driver/URL → `BeanCreationException`.
`@WebMvcTest` (Student/Class controller) không bị ảnh hưởng vì không load JPA.
=> Sửa bằng thêm `@ActiveProfiles("dev")` vào `StudentApiApplicationTests` — test này giờ
**phụ thuộc Postgres container đang chạy** (`docker compose up -d`) khi chạy `mvn test`,
đánh đổi hợp lý ở giai đoạn học, nhưng cần nhớ: nếu sau này có CI hoặc máy khác không có
Docker, test này sẽ fail — lúc đó cần Testcontainers hoặc H2 riêng cho test, chưa làm ở
buổi này.

## Lưu ý cho buổi sau

- `ddl-auto: update` chỉ tạm thời cho dev, tuần 6 chuyển sang Flyway migration — lúc đó đổi
  lại thành `validate`.
