# Buổi 21 — Lỗi N+1

Mục tiêu 30 phút đầu: tạo lỗi N+1 thật trên `GET /api/classes` (trả kèm danh
sách sinh viên mỗi lớp qua quan hệ LAZY), để sau đó tự đếm số câu SQL phát
sinh với 10 lớp trước khi học cách sửa (buổi sau).

## Hiện trạng trước khi sửa

`SchoolClass` đã có sẵn `@OneToMany(mappedBy = "schoolClass", fetch =
FetchType.LAZY) students`, nhưng chưa có chỗ nào map field này ra DTO —
`ClassResponse` trước đó chỉ có `id`, `name`, nên `GET /api/classes` chưa hề
đụng tới quan hệ này, chưa có N+1.

## Code (AI viết, theo yêu cầu)

- `dto/ClassResponse.java`: thêm field `List<StudentResponse> students`.
- `mapper/ClassMapper.java`: thêm `uses = StudentMapper.class` vào
  `@Mapper` để MapStruct tự sinh code map `List<Student> → List<StudentResponse>`
  bằng `StudentMapper.toResponse` đã có sẵn.
- Không đụng Controller/Service/Repository, không đổi schema, không thêm
  dependency đếm SQL (p6spy, datasource-proxy...) — log `show-sql: true` sẵn
  có ở profile `dev` (`application-dev.yml`) là đủ để đếm số câu SELECT bằng
  mắt.
- Sửa `ClassControllerTest` (2 chỗ gọi `new ClassResponse(id, name)`) cho
  khớp constructor record 3 tham số mới — chỉ để code compile, không đổi
  logic test.

## Vì sao đoạn code này tạo ra N+1

`spring.jpa.open-in-view` không bị override ở `application.yml` /
`application-dev.yml` → giữ mặc định `true` của Spring Boot, nghĩa là session
Hibernate vẫn mở xuyên suốt request (kể cả lúc serialize JSON ở Controller).
Nhờ vậy, khi `ClassMapperImpl` (code MapStruct tự sinh) gọi
`schoolClass.getStudents()` cho từng lớp trong danh sách trả về của
`GET /api/classes`, Hibernate lazy-load riêng từng collection đó — tức là:

- 1 câu `SELECT` lấy toàn bộ lớp (`classRepository.findAll()`).
- N câu `SELECT ... FROM students WHERE class_id = ?` — mỗi lớp 1 câu riêng,
  theo đúng thứ tự lặp trong `ClassMapperImpl.studentListToStudentResponseList(...)`.

→ Tổng cộng N+1 câu SQL cho N lớp, thay vì 1-2 câu nếu dùng `JOIN FETCH`
hoặc `@EntityGraph`.

## Build & test

- `mvn -o compile`: sạch.
- Xem code MapStruct tự sinh (`target/generated-sources/.../ClassMapperImpl.java`):
  xác nhận đúng vòng lặp gọi `getStudents()` per-class như phân tích ở trên.
- `mvn -o test -Dtest=ClassControllerTest`: pass, không regression.

## Thực nghiệm đã làm (5 lớp, dữ liệu dev sẵn có)

Người học đã tự chạy app qua nút Run của IntelliJ (Run Configuration
`StudentApiApplication(dev)`), gọi `GET /api/classes`, đếm số khối
`Hibernate:` trong Console — xác nhận đúng **6 câu SQL** cho 5 lớp (1 + 5),
khớp N+1 như phân tích ở trên.

Gặp 1 sự cố môi trường không liên quan logic bài học: lần chạy sau đó báo
`Port 8081 was already in use` vì còn 1 tiến trình Java cũ (từ lần Run trước
đó) chưa tắt — xử lý bằng cách tìm PID qua `Get-NetTCPConnection -LocalPort
8081` rồi `Stop-Process -Force`.

## 45 phút — Sửa N+1: so sánh `JOIN FETCH` qua `@Query` vs `@EntityGraph`

### Code (AI viết, theo yêu cầu) — thử nghiệm tạm trong `ClassRepository`

Thêm tạm 3 method để so sánh (không commit cả 3, chỉ giữ 1 cách cuối cùng):

```java
@Query("SELECT DISTINCT c FROM SchoolClass c LEFT JOIN FETCH c.students")
List<SchoolClass> findAllWithStudentsJoinFetch();

@EntityGraph(attributePaths = "students")
@Query("SELECT c FROM SchoolClass c")
List<SchoolClass> findAllWithStudentsEntityGraph();

@Query("SELECT DISTINCT c FROM SchoolClass c LEFT JOIN FETCH c.students")
Page<SchoolClass> findAllWithStudentsJoinFetchPaged(Pageable pageable);
```

Mỗi lần đổi `ClassService.getAll()` sang gọi 1 trong 3 method trên, chạy
app thật ở **port 8082** (`mvn spring-boot:run
-Dspring-boot.run.arguments=--server.port=8082`) để không đụng app đang
chạy ở 8081 trên IntelliJ của người học, gọi `GET /api/classes` bằng `curl`,
đếm số khối `Hibernate:` trong log.

### Kết quả đo được (5 lớp)

| Cách | Số câu SQL |
|---|---|
| `findAll()` cũ (LAZY, bug ban đầu) | 6 (1 + 5) |
| `JOIN FETCH` thủ công qua `@Query` | **1** |
| `@EntityGraph(attributePaths = "students")` | **1** |
| `JOIN FETCH` + `Pageable` (`PageRequest.of(0, 3)`) | 2 (1 nội dung + 1 `count`) |

`JOIN FETCH` và `@EntityGraph` cho **cùng 1 câu SQL** (Hibernate sinh ra
y hệt nhau), khác biệt chỉ ở cách khai báo: `@EntityGraph` để Spring Data
tự thêm phần fetch, không phải tự viết JPQL.

### Phát hiện khác với giả định ban đầu của bài — không thấy cảnh báo "in memory"

Bài yêu cầu "đọc cảnh báo của Hibernate về phân trang trong bộ nhớ" khi dùng
`JOIN FETCH` kèm `Pageable` — đây là hành vi kinh điển của Hibernate 5.x
(`HHH000104: firstResult/maxResults specified with collection fetch;
applying in memory!`, tức Hibernate phải load hết bảng về RAM rồi tự cắt
trang bằng tay vì không thể `LIMIT` đúng khi JOIN 1-N làm nhân bản dòng).

Nhưng chạy thật trên project này (Spring Boot 4.1.2, Hibernate ORM bản đi
kèm) thì **không xuất hiện cảnh báo đó**. Xem SQL thực tế sinh ra:

```sql
select ... from (
    select distinct sc1_0.id, sc1_0.created_at, sc1_0.name
    from classes sc1_0
    offset ? rows fetch first ? rows only
) sc1_0(id, created_at, name)
left join students s1_0 on sc1_0.id = s1_0.class_id
```

Hibernate hiện đại (ORM 7.x trong Spring Boot 4) tự viết lại truy vấn thành
subquery: phân trang `SchoolClass` bằng `OFFSET ... FETCH FIRST` **ở mức
SQL** trước, rồi mới `LEFT JOIN` bảng `students` vào kết quả đã phân trang
sẵn — tránh được vấn đề nhân bản dòng nên không cần fallback in-memory nữa.
Đây là cải tiến thật của phiên bản Hibernate đang dùng, không phải sai sót
khi đo — ghi lại đúng như quan sát thay vì ép kết quả khớp giả định ban đầu
của đề bài (tài liệu/StackOverflow nói về cảnh báo này phần lớn viết cho
Hibernate 5.x).

### Chốt phương án cuối

Người học chọn **`@EntityGraph`** (ngắn gọn hơn, không cần tự viết JPQL) —
giữ lại 1 method duy nhất `ClassRepository.findAllWithStudents()`, xoá
`findAllWithStudentsJoinFetch` và `findAllWithStudentsJoinFetchPaged` khỏi
code (không commit, chỉ còn trong log này làm tư liệu so sánh).
`ClassService.getAll()` gọi method này thay vì `findAll()`.

### Build & test

- `mvn -o compile`: sạch sau khi chốt phương án cuối.
- Verify app thật (port 8082): `GET /api/classes` → đúng **1 câu SQL**.
- `mvn test` (toàn bộ suite, có Docker + DB dev): **55/55 pass**, không
  regression.

## 15 phút — Nhờ AI viết 1 endpoint có quan hệ, kiểm tra có dính N+1 không

Mục đích: không nói trước AI phải tối ưu gì, để xem cách viết "theo bản năng"
có tự nhiên dính N+1 hay không, rồi ghi lại quan sát thật — khác với 2 phần
trên (`GET /api/classes`), phần này **chưa sửa**, chỉ viết + phát hiện.

### Chọn endpoint

`GET /api/students/{id}/courses` — danh sách môn học + điểm của 1 sinh viên,
đi qua `Student → Enrollment → Course`. Đây là nhánh quan hệ project chưa có
(trước đó `EnrollmentController` chỉ có `POST /api/enrollments`, chưa có GET
nào cả).

### Code (AI viết, không cố ý tối ưu trước)

- `dto/StudentCourseResponse.java`: `record StudentCourseResponse(Long
  courseId, String courseName, Double score)`.
- `repository/EnrollmentRepository.java`: thêm derived query
  `List<Enrollment> findByStudentId(Long studentId)` — theo đúng pattern có
  sẵn (`existsByStudentIdAndCourseId` đã chứng minh nested property
  `student.id` tự resolve được qua tên `StudentId`).
- `service/EnrollmentService.java`: thêm `getCoursesByStudentId(studentId)`
  — check sinh viên tồn tại (404 nếu không), lấy danh sách `Enrollment` theo
  `studentId`, rồi `map` từng cái sang `StudentCourseResponse` bằng
  `e.getCourse().getId()` / `e.getCourse().getName()`.
- `controller/StudentController.java`: thêm `GET
  /api/students/{id}/courses`, inject thêm `EnrollmentService` vào
  constructor (theo đúng pattern `ClassController` đã inject 2 service).
- Sửa `StudentControllerTest`: thêm `@MockitoBean EnrollmentService` (vì
  `StudentController` giờ có thêm dependency qua constructor) — chỉ để
  compile/pass context, không đổi logic test có sẵn.

### Kết quả kiểm tra — có dính N+1 thật

Tạo tạm 3 course + enroll 1 sinh viên (id=3) vào cả 3, gọi
`GET /api/students/3/courses` trên app test (port 8082), đếm log:

**5 câu SQL cho 3 enrollment:**
1. `SELECT count(*) FROM students WHERE id=?` (check tồn tại sinh viên).
2. `SELECT ... FROM enrollments WHERE student_id=?` (lấy danh sách
   enrollment).
3–5. **3 câu `SELECT ... FROM courses WHERE id=?` riêng lẻ** — mỗi lần gọi
   `e.getCourse().getName()` trong `.map(...)` làm Hibernate lazy-load
   `Course` của từng `Enrollment` một, vì `Enrollment.course` là
   `@ManyToOne(fetch = FetchType.LAZY)`.

→ Đúng là N+1 (1 + N với N = số enrollment), **không phải cố ý nhồi vào** —
đây là cách viết tự nhiên nhất khi cần lấy tên course từ 1 danh sách
enrollment, và nó tự dính lỗi y hệt pattern ở `GET /api/classes` lúc đầu
buổi. Bài học: quan hệ LAZY nào bị truy cập trong vòng lặp/`map()` ở tầng
Service đều là điểm nghi ngờ N+1 đầu tiên cần soi, bất kể entity nào.

### Build & test

- `mvn -o compile`: sạch.
- `mvn test` (toàn bộ suite): **55/55 pass** sau khi thêm `@MockitoBean
  EnrollmentService` vào `StudentControllerTest`.

### Chưa làm trong 15 phút này

- Chưa sửa N+1 của endpoint này (để dành bài tập riêng — áp dụng lại
  `JOIN FETCH`/`@EntityGraph` như đã học ở phần 45 phút, nhưng lần này trên
  `EnrollmentRepository` thay vì `ClassRepository`).
- Dữ liệu test (3 course "Toan roi rac", "Lap trinh Java", "Co so du lieu" +
  3 enrollment của sinh viên id=3) đang nằm trong DB dev, chưa dọn — giữ lại
  để tiện test lại bước sửa N+1 sau.

## Phần DB song song (45-60 phút) — 1.000 SELECT nhỏ vs 1 JOIN

File: `sql/week-7/16-select-n-vs-join.sql`.

**Override theo yêu cầu người học** ("override lần này, AI viết thẳng cả
query/EXPLAIN, tôi review") — khác mặc định "DB song song" (AI chỉ sinh dữ
liệu mẫu, phần EXPLAIN/query để người học tự gõ). AI viết toàn bộ file, tự
chạy thật qua `docker exec ... psql` trên Postgres dev, dán nguyên kết quả
thật (không bịa số).

### Dữ liệu mẫu (Phần 0)

Sinh 1.000 lớp (`Buoi21-Lop1`..`Buoi21-Lop1000`) + 5.000 sinh viên (5/lớp) —
đủ lớn để "1.000 câu SELECT nhỏ" có nghĩa thật (1 SELECT/lớp). `ANALYZE`
ngay sau bulk insert (bài học từ buổi 19: stats cũ làm planner chọn sai kế
hoạch).

### Kết quả đo thật

| Cách | Số câu SQL | Thời gian (server-side, không qua network) |
|---|---:|---:|
| 1.000 SELECT nhỏ (vòng lặp PL/pgSQL `DO $$`) | 1.000 | 67.706 ms |
| 1 câu JOIN duy nhất | 1 | 15.493 ms |

→ **JOIN nhanh hơn ~4.4 lần**, dù đang ở tình huống bất lợi: JOIN phải `Seq
Scan` toàn bộ bảng `students` (105.000 dòng tích luỹ từ các buổi trước, lọc
bằng `LIKE 'Buoi21-Lop%'` trên `classes.name` không có index hỗ trợ), trong
khi mỗi câu SELECT nhỏ đều tận dụng được `idx_students_class_id` (tạo từ
buổi 15). Tức là ưu thế của JOIN ở đây **không đến từ việc mỗi query riêng
rẻ hơn** (Phần 1 đo 1 câu SELECT nhỏ chỉ ~0.1ms, rất rẻ) — mà đến từ việc
**giảm số lần round-trip + planning từ 1.000 xuống còn 1**. Số đo này cũng
chỉ là cận dưới: không tính round-trip mạng thật giữa app và DB qua JDBC
(khác với DO block chạy gọn trong 1 session Postgres) — nên trong ứng dụng
thật (như N+1 ở phần Spring buổi này), chênh lệch thực tế còn lớn hơn số đo
ở đây.

Liên hệ trực tiếp: N+1 ở `GET /api/classes` (phần Spring buổi này) chính là
hiện tượng "1.000 SELECT nhỏ" này nhưng quy mô nhỏ hơn (5 lớp → 6 câu SQL);
`JOIN FETCH`/`@EntityGraph` là cách ép Hibernate sinh đúng 1 câu JOIN thay vì
N+1 câu.

## Người học tự làm

- Thử thêm `@BatchSize(size = N)` trên `students` ở `SchoolClass` thay vì
  `JOIN FETCH`/`@EntityGraph`, so sánh số câu SQL khi N lớn/nhỏ hơn số lớp
  thực tế — hiểu khác biệt giữa "gộp 1 query duy nhất" (JOIN FETCH) và "chia
  nhiều batch nhỏ" (`@BatchSize`).
- Thử tái hiện lại cảnh báo `in memory` kinh điển bằng cách đổi sang entity
  có quan hệ N-N (`Student`-`Course` qua `Enrollment`, đã có từ buổi 16) với
  `JOIN FETCH` + `Pageable`, xem Hibernate bản hiện tại có còn tối ưu được
  subquery như case 1-N này không, hay bắt buộc fallback in-memory.
