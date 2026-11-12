# Buổi 16 — Thứ Tư 25/11/2026 — Quan hệ N-N qua Enrollment

## Entity `Course` + CRUD `/api/courses` (AI làm toàn bộ, theo yêu cầu)

- `entity/Course.java`: theo đúng pattern `SchoolClass` (buổi 13) — class
  `@Entity`, Lombok `@Getter/@Setter/@NoArgsConstructor`, `id`, `name`
  (`unique = true`), `createdAt`, constructor 3 tham số viết tay (không dùng
  `@AllArgsConstructor` vì không có field quan hệ để tránh tương tự
  `SchoolClass.students`).
- `dto/CourseRequest.java` (`@NotBlank @Size(max=100) name`),
  `dto/CourseResponse.java` (`id, name`), `mapper/CourseMapper.java`
  (MapStruct interface, chỉ map thẳng field).
- `repository/CourseRepository.java`: `existsByNameIgnoreCase`,
  `existsByNameIgnoreCaseAndIdNot` — y hệt `ClassRepository`.
- `exception/DuplicateCourseNameException.java` (409) — **không** tái dùng
  `DuplicateClassNameException` của lớp học dù logic giống hệt, vì theo
  quy ước đặt tên exception nghiệp vụ gắn với từng entity, dùng nhầm tên sẽ
  gây hiểu lầm khi đọc log/response lỗi.
- `service/CourseService.java`, `controller/CourseController.java`: CRUD đầy
  đủ (`GET` list/detail, `POST`, `PUT`, `DELETE`) — sao y pattern
  `ClassService`/`ClassController`.
- `GlobalExceptionHandler`: thêm handler `DuplicateCourseNameException` →
  409, cạnh handler `DuplicateClassNameException` đã có.

## Entity `Enrollment` (quan hệ N-N Student ↔ Course)

- `entity/Enrollment.java`: bảng trung gian tường minh (không dùng
  `@ManyToMany` thuần) vì có thêm cột nghiệp vụ `score` — đúng khuyến nghị
  chuẩn JPA khi quan hệ N-N có dữ liệu phụ.
  - `@ManyToOne(fetch = LAZY) student` (`@JoinColumn(name = "student_id")`),
    `@ManyToOne(fetch = LAZY) course` (`@JoinColumn(name = "course_id")`).
  - `score`: `Double`, nullable (chưa chấm điểm lúc mới đăng ký).
  - `@Table(uniqueConstraints = @UniqueConstraint(columnNames =
    {"student_id", "course_id"}))` — chặn đăng ký trùng ở tầng DB, không chỉ
    dựa vào check ở Service (an toàn trước race condition 2 request đăng ký
    đồng thời).
- `dto/EnrollmentRequest.java` (`studentId`, `courseId` bắt buộc; `score`
  optional, `@DecimalMin(0.0) @DecimalMax(10.0)` khi có truyền),
  `dto/EnrollmentResponse.java` (`id, studentId, courseId, score,
  createdAt`), `mapper/EnrollmentMapper.java` (map `student.id` →
  `studentId`, `course.id` → `courseId`, giống cách `StudentMapper` map
  `schoolClass.id` → `classId` ở buổi 15 — tránh N+1 query).
- `repository/EnrollmentRepository.java`: `existsByStudentIdAndCourseId`.

## `POST /api/enrollments` — tự viết (không giao AI, theo yêu cầu)

- `service/EnrollmentService.create()`: tra `Student` theo `studentId` (404
  `NotFoundException` nếu không có), tra `Course` theo `courseId` (404 nếu
  không có), kiểm tra `existsByStudentIdAndCourseId` → 409
  `DuplicateEnrollmentException` nếu đã đăng ký, sau đó mới `save`.
- `controller/EnrollmentController`: chỉ có `POST /api/enrollments`, trả
  `201 Created` + `Location` header (cùng pattern `ServletUriComponentsBuilder`
  như `StudentController`/`ClassController`). **Không** thêm `GET`
  list/detail — ngoài phạm vi yêu cầu buổi này.
- `exception/DuplicateEnrollmentException.java` + handler trong
  `GlobalExceptionHandler` → 409.

## Build & test

- `mvn compile`: sạch, không lỗi.
- `mvn test`: 30/30 test `@WebMvcTest` cũ (Student/Class) vẫn pass, không bị
  regression. Riêng `StudentApiApplicationTests.contextLoads` (context load
  thật qua Postgres container `student-api-postgres`) fail trong môi trường
  chạy AI lần này vì Docker Desktop không chạy (`docker ps` lỗi kết nối
  daemon) — **không liên quan thay đổi code buổi này**, chỉ là máy đang chạy
  AI không có Postgres container lên sẵn. Người học tự `docker compose up -d`
  rồi chạy lại `mvn test` để xác nhận `contextLoads` qua (và để Hibernate
  `ddl-auto: update` tự sinh bảng `courses`, `enrollments` mới).

## Người học tự làm

- Tự kiểm chứng Hibernate sinh đúng schema (`courses`, `enrollments` + FK +
  unique constraint `uk_enrollment_student_course`) sau khi chạy app thật với
  Postgres.
- Tự viết test cho `POST /api/enrollments` (happy path 201, 404 sinh viên/
  khóa học không tồn tại, 409 đăng ký trùng, 400 thiếu `studentId`/`courseId`)
  — theo checklist `@WebMvcTest` đã quy ước trong `CLAUDE.md`, chưa làm ở
  buổi này (AI chỉ viết code sản xuất, chưa viết test cho phần tự viết này
  theo đúng phạm vi được giao).
- Đối chiếu `docs/sql/` nếu muốn — track SQL riêng chưa cập nhật bảng
  `courses`/`enrollments` mới.

## DB song song — `sql/week-5/13-enrollments-multicolumn-index.sql`

Đề bài: sinh 100.000 dòng `enrollments` bằng `generate_series`, tạo index
nhiều cột `(student_id, course_id)`, so sánh EXPLAIN ANALYZE (Seq Scan vs
Index Scan) khi lọc CHỈ theo `course_id`.

Theo đúng quy ước "DB song song" (không override lần này — khác buổi 15),
AI **chỉ viết Phần 0** (seed thêm `courses`/`students` + insert 100.000 dòng
`enrollments`). Phần 1 trở đi (tạo index, chọn thứ tự cột, chạy/đọc EXPLAIN
ANALYZE, so sánh lọc theo `course_id` vs `student_id`) để TODO kèm câu hỏi
gợi ý, người học tự làm.

### 2 lỗi AI tự mắc phải khi thử chạy Phần 0 (tự phát hiện, tự sửa trước khi giao)

1. **Guard trùng dữ liệu sai cách**: bản đầu dùng 1 email cố định
   (`sv1nguyen.an@gmail.com`) làm điều kiện `WHERE NOT EXISTS` để tránh seed
   `students` 2 lần — giống hệt pattern ở `sql/week-5/12-index-fk.sql`. Nhưng
   DB thật đã có sẵn 5000 dòng `students` seed bằng pattern email KHÁC
   (`...@gmail.com` không có tiền tố `sv{g}`), nên guard không nhận ra dữ
   liệu đã đủ, chạy xong insert thêm NGUYÊN 5000 dòng ảo trùng lặp (tổng
   thành 10000). AI tự phát hiện qua `SELECT COUNT(*)` trước/sau, tự
   `DELETE FROM students WHERE email LIKE 'sv%@gmail.com'` dọn lại, rồi sửa
   guard sang đếm tổng `(SELECT COUNT(*) FROM students) < 5000` (đáng tin cậy
   hơn vì không phụ thuộc đúng 1 giá trị email cụ thể của dữ liệu cũ).
2. **Sai thứ tự `LIMIT` / `ON CONFLICT` trong câu `INSERT ... SELECT`**: bản
   đầu viết `... CROSS JOIN courses c ON CONFLICT (...) DO NOTHING LIMIT
   100000;` — tưởng `LIMIT` áp dụng được ở bất kỳ đâu trước dấu `;`, nhưng cú
   pháp `INSERT` của Postgres là `INSERT ... query [ON CONFLICT ...]
   [RETURNING ...]`, và `LIMIT` phải nằm TRONG `query` (tức trước `ON
   CONFLICT`), không phải sau. Chạy thử bị lỗi
   `duplicate key value violates unique constraint
   "uk_enrollment_student_course"` ngay dòng đầu tiên (do `ON CONFLICT` thực
   ra áp dụng cho 1 query không có `LIMIT`, không khớp ý định — xác nhận lại
   bằng cách tách câu `INSERT` ra chạy riêng trong 1 transaction test
   `BEGIN; ... ROLLBACK;`). Sửa lại đúng thứ tự: `... LIMIT 100000 ON
   CONFLICT (student_id, course_id) DO NOTHING;`.
3. **Lỗi kiểu dữ liệu ẩn sau khi sửa guard #1**: đổi
   `generate_series(1, 5000 - (SELECT COUNT(*) FROM students))` — vế phải là
   `bigint` (vì `COUNT(*)` trả `bigint`), khiến biến vòng lặp `g` cũng trở
   thành `bigint`, và biểu thức `DATE '2000-01-01' + (g * 17)` (vốn chạy được
   khi `g` là `int`, dùng ở `12-index-fk.sql`) báo lỗi `operator does not
   exist: date + bigint`. Sửa bằng ép kiểu tường minh
   `(5000 - (SELECT COUNT(*) FROM students))::int` để `generate_series` chọn
   overload `int`, giữ `g` là `int` như cũ.

Sau khi sửa cả 3 chỗ, chạy lại Phần 0 2 lần liên tiếp để xác nhận **idempotent**
(lần 2 insert 0 dòng mới, không lỗi) — kết quả cuối: 5000 students, 31
courses, 100.000 enrollments.

### Người học tự làm (Phần 1-4, chưa làm)

- Tự chạy `EXPLAIN ANALYZE` lọc `WHERE course_id = ...` TRƯỚC khi có index
  nhiều cột — dự đoán trước Seq Scan hay Index Scan (gợi ý: chỉ có index
  ngầm từ `UNIQUE(student_id, course_id)`, theo leftmost-prefix rule).
- Tự tạo 1 index nhiều cột, tự chọn thứ tự cột dựa trên câu hỏi "lọc theo
  course_id một mình thì cột nào nên đứng trước".
- Tự chạy lại EXPLAIN ANALYZE sau khi có index, so sánh cost/actual time/loại
  node quét.
- Tự thử lại với điều kiện lọc theo `student_id` để so sánh.
