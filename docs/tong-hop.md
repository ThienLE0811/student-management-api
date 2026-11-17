# Tổng hợp — tự kiểm tra cuối khóa

Đối chiếu từng mục với bằng chứng thật trong repo (code/commit/file SQL),
không chỉ trả lời lý thuyết suông.

## ☑ Giải thích IoC/DI và vòng đời của một request trong Spring

**IoC (Inversion of Control)**: thay vì code tự `new` ra dependency của nó,
việc tạo và quản lý vòng đời object (bean) được giao cho Spring container.
Code chỉ khai báo "tôi cần cái gì", container quyết định "đưa cái gì vào,
lúc nào".

**DI (Dependency Injection)** là cách IoC được hiện thực: container tiêm
dependency vào class qua constructor. Trong repo này dùng **constructor
injection** (theo đúng quy ước ở `CLAUDE.md`) — ví dụ `StudentController`,
`StudentService` khai `private final` field + `@RequiredArgsConstructor`
(Lombok sinh constructor), Spring tự nhận ra đúng 1 constructor và tiêm
`StudentService`, `StudentMapper`... vào mà không cần `@Autowired` tường
minh.

**Vòng đời 1 request** (ví dụ `POST /api/students`):

1. `DispatcherServlet` nhận HTTP request, qua các `Filter` đã đăng ký
   (repo có `filter/RequestTimingFilter` đo thời gian xử lý request).
2. `HandlerMapping` tra route tới đúng method trong `StudentController`
   (khớp `@PostMapping`, path `/api/students`).
3. Spring MVC bind JSON body → `StudentRequest` (`record`), chạy
   `@Valid` — nếu sai (vd `email` không hợp lệ) ném
   `MethodArgumentNotValidException` **trước khi** vào logic Controller.
4. Controller gọi `StudentService` (business logic: check trùng email,
   check lớp tồn tại...), Service gọi `StudentRepository`
   (`JpaRepository`) để chạm DB qua Hibernate.
5. Service trả `Student` (entity) → Controller **không** trả entity thẳng
   ra ngoài, luôn map sang `StudentResponse` qua `StudentMapper`
   (MapStruct) trước khi trả `ResponseEntity`.
6. Nếu có exception ở bất kỳ bước nào (404, 409, 400, 500...),
   `GlobalExceptionHandler` (`@RestControllerAdvice`) bắt và format lại
   thành `ProblemDetail` thống nhất, thay vì để lộ exception gốc.

## ☑ Viết API CRUD có DTO, validation và format lỗi chuẩn

Đã có đủ qua các entity chính (`Student`, `SchoolClass`, `Course`,
`Enrollment`) — xem chi tiết danh sách endpoint đầy đủ ở `README.md` (mục
"API Endpoints"). Tóm tắt bằng chứng:

- **DTO**: toàn bộ request/response là `record` (`dto/StudentRequest`,
  `dto/StudentResponse`...), validation gắn trực tiếp lên từng field
  (`@NotBlank`, `@Size`, `@Email`, `@Past`, `@DecimalMin/@DecimalMax`,
  `@Positive`...).
- **Format lỗi chuẩn**: toàn bộ lỗi đi qua `GlobalExceptionHandler`, trả
  `org.springframework.http.ProblemDetail`:
  - `NotFoundException` → 404.
  - `DuplicateEmailException`/`DuplicateClassNameException`/
    `DuplicateCourseNameException`/`DuplicateEnrollmentException`/
    `ClassFullException` → 409.
  - `ObjectOptimisticLockingFailureException` (buổi 22) → 409.
  - `MethodArgumentNotValidException` → 400, kèm property `errors` (map
    field → message).
  - `NoResourceFoundException` (route không tồn tại) → 404 (fix ở buổi 20).
  - Exception khác → 500, có `log.error` trước, không lộ stacktrace gốc.
- **Test**: mỗi Controller có `@WebMvcTest` + `MockMvc`, mock Service bằng
  `@MockitoBean`, cover đủ happy path / 404 / 400 (từng field) / 409.

## ☑ Giải thích lỗi N+1 và hai cách sửa; `@Transactional` đặt ở đâu và khi nào không rollback

**N+1** đã tái hiện thật ở buổi 21 (`GET /api/classes`): load N lớp bằng 1
query, sau đó mỗi lớp lazy-load `students` riêng → 1 + N SELECT. Đo thật
bằng SQL thuần ở `sql/week-7/16-select-n-vs-join.sql` (1.000 lớp, so
1.000 câu SELECT nhỏ với 1 câu JOIN, đọc `EXPLAIN ANALYZE` cả hai).

**Hai cách sửa đã dùng:**

1. `@EntityGraph` trên repository method — khai báo fetch join ngay trên
   derived query, Spring Data tự gộp vào 1 câu SQL.
2. `JOIN FETCH` viết tay trong JPQL (`@Query`) — kiểm soát rõ câu SQL sinh
   ra, dễ thấy lỗi `MultipleBagFetchException` hơn nếu fetch nhầm 2
   collection cùng lúc.

**`@Transactional` đặt ở đâu**: ở tầng **Service** (business logic), không
đặt ở Controller/Repository — ví dụ `StudentService.addToClass` (buổi 20,
22) bọc toàn bộ chuỗi thao tác (đọc lớp cũ/mới, update `enrolledCount`,
ghi `ClassTransferHistory`) trong 1 transaction để đảm bảo tất cả cùng
commit hoặc cùng rollback.

**Khi nào không rollback:**

- Mặc định chỉ rollback với **unchecked exception** (`RuntimeException`/
  `Error`); checked exception không tự rollback trừ khi khai `rollbackFor`.
- Exception bị **catch và nuốt** trong method transactional.
- Gọi method `@Transactional` từ method khác **trong cùng class**
  (self-invocation) — proxy Spring bị bỏ qua.
- Transaction con (`REQUIRES_NEW`) lỗi không tự rollback transaction cha.

(Xem thêm `docs/buoi-23.md` mục 1–3 cho phần trả lời chi tiết hơn, có đối
chiếu với `GlobalExceptionHandler` thật.)

## ☑ Viết câu SQL có JOIN, GROUP BY, subquery; đọc EXPLAIN cơ bản; tự thiết kế 4–5 bảng có quan hệ

**JOIN**: `sql/week-3/07-join.sql` — INNER JOIN (`students` ↔ `classes`),
LEFT JOIN (sinh viên chưa có lớp), JOIN 3 bảng qua bảng trung gian
(`students` ↔ `enrollments` ↔ `courses`).

**GROUP BY / HAVING**: `sql/week-4/09-group-by.sql` — sĩ số mỗi lớp
(`COUNT` + `GROUP BY`), điểm trung bình mỗi khóa học (`AVG` + `GROUP BY`),
lớp có ≥10 sinh viên (`HAVING COUNT(...) > 10`).

**Subquery**: dùng `NOT EXISTS` kiểu correlated subquery, cả ở SQL thuần
(`WHERE NOT EXISTS (SELECT 1 FROM classes WHERE ...)` — guard chống insert
trùng ở nhiều file `sql/week-5`, `sql/week-6`) và ở JPQL thật trong code
ứng dụng: `StudentRepository.findStudentsWithoutCourse()`:

```java
@Query("SELECT s FROM Student s WHERE NOT EXISTS (SELECT 1 FROM Enrollment e WHERE e.student = s)")
List<Student> findStudentsWithoutCourse();
```

(phục vụ endpoint `GET /api/students/without-course`).

**Đọc `EXPLAIN` cơ bản**: đã thực hành ở buổi 15 (`sql/week-5/12-index-fk.sql`
— so `Seq Scan` trước khi có index trên `students.class_id` với `Index Scan`
sau khi tạo index), buổi 16 (`sql/week-5/13-enrollments-multicolumn-index.sql`
— thứ tự cột trong index nhiều cột ảnh hưởng tới việc planner có dùng được
index khi chỉ lọc theo cột thứ hai không), buổi 21
(`sql/week-7/16-select-n-vs-join.sql` — `EXPLAIN ANALYZE` so sánh N+1 SELECT
nhỏ với 1 JOIN).

**Tự thiết kế 4–5 bảng có quan hệ**: schema thật hiện có 5 bảng (nguồn sự
thật là entity Java + Flyway migration, xem ERD đầy đủ ở `README.md`):

- `classes` (`SchoolClass`) — 1–n với `students`.
- `students` (`Student`) — n–1 với `classes` (`class_id NOT NULL`).
- `courses` (`Course`) — n–n với `students` qua bảng trung gian.
- `enrollments` (`Enrollment`) — bảng trung gian `students` ↔ `courses`,
  `UNIQUE(student_id, course_id)` chặn ghi danh trùng.
- `class_transfer_history` (`ClassTransferHistory`) — ghi log mỗi lần
  sinh viên đổi lớp (`from_class_id` → `to_class_id`), thêm ở buổi 20.

Bản thiết kế tay đầu tiên (chưa có `class_transfer_history`) ở
`sql/week-3/06-schema.sql` (4 bảng: `students`, `classes`, `courses`,
`enrollments`).

## ☑ Dùng SQL thuần mô phỏng transaction rollback và lost update, sửa bằng `FOR UPDATE`

**Rollback**: `sql/week-3/04-update-delete.sql` — 3 case `BEGIN` → thao tác
(`UPDATE`/`DELETE`, có cả case cố tình quên `WHERE` để thấy hậu quả) →
`SELECT` xác nhận thay đổi đã áp dụng trong transaction → `ROLLBACK` → dữ
liệu trở lại như cũ, chứng minh thay đổi chưa `COMMIT` thì chưa vĩnh viễn.

**Lost update + sửa bằng `FOR UPDATE`**: thực hiện ở buổi 22 (DB, override
"AI làm, người học review" — xem đầy đủ ở `docs/ai-review-log/buoi-22.md`),
dùng 2 session `psql -f` chạy song song (đồng bộ thời điểm bằng
`pg_sleep`) trên 1 bảng tạm `demo_slot(remaining_slots)`:

- Không khóa: S1 đọc `remaining_slots=1`, sleep, `UPDATE = 0`, `COMMIT`.
  S2 đọc `remaining_slots=1` (stale, trước khi S1 commit), rồi vẫn
  `UPDATE = 0` dựa trên giá trị cũ đã đọc → **lost update thật**: giá trị
  cuối vẫn là 0 dù có 2 lượt "ghi danh", không có lỗi nào được báo ra.
- Sửa bằng `SELECT ... FOR UPDATE`: lặp lại kịch bản, S2 bị **block thật**
  tới khi S1 commit (đo được độ trễ ~0.4s), sau đó đọc được giá trị **mới
  nhất** thay vì giá trị stale — hết lost update.

Đối chiếu thêm ở tầng Java cùng buổi: `addToClass` (optimistic `@Version`,
trả `409` lúc commit) vs `addToClassPessimistic` (`ClassRepository.findByIdForUpdate`
dùng `@Lock(PESSIMISTIC_WRITE)`, chặn ngay lúc đọc) — bảng so sánh chi tiết
ở `docs/ai-review-log/buoi-22.md`.
