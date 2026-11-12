# Buổi 17 — Thứ Năm 26/11/2026 — JPQL, @Query, native query, window function

## DB song song — `sql/week-5/14-jpql-query.sql` (AI viết thẳng, có override)

Theo đúng quy ước "DB song song" (`CLAUDE.md`), AI mặc định chỉ để TODO + gợi
ý cho phần viết SQL thuần. Người học đã **override tường minh 2 lần** ("bạn
viết thẳng rồi tôi review") — 1 lần cho Phần 1-2, 1 lần riêng cho Phần 3 (window
function) — nên AI viết thẳng cả 3 phần thay vì để TODO:

- **Phần 1** — điểm trung bình theo khóa: `LEFT JOIN` từ `courses` (không phải
  từ `enrollments`) để khóa chưa ai đăng ký vẫn hiện ra với `average_score =
  NULL` — `AVG()` tự bỏ qua `NULL`, không coi là 0.
- **Phần 2** — sinh viên chưa đăng ký khóa nào: viết cả 2 cách (`NOT EXISTS`
  và `LEFT JOIN ... IS NULL`) kèm `EXPLAIN ANALYZE` để so sánh plan — phần so
  sánh kết quả thực tế vẫn để TODO cho người học tự chạy.
- **Phần 3** — window function: `ROW_NUMBER()` vs `RANK()` (`PARTITION BY
  course_id ORDER BY score DESC`, bọc CTE rồi lọc `<= 3` ở ngoài vì window
  function không lọc được trực tiếp bằng `WHERE`), và `AVG(score) OVER
  (PARTITION BY course_id)` so với `score` từng dòng.

**Chưa verify**: AI không chạy các câu SQL này trên DB thật (không có quyền
kết nối Postgres dev trực tiếp trong phiên làm việc) — người học cần tự chạy
trong DBeaver, đặc biệt đối chiếu `ROW_NUMBER()` vs `RANK()` khi có đồng điểm
(data random 100k dòng từ buổi 16 có thể không tự nhiên có đồng điểm ở đúng
ranh giới top 3 — có thể cần tự `UPDATE` vài dòng để test).

## JPQL `@Query` — điểm trung bình theo khóa, sinh viên chưa đăng ký (AI viết, theo yêu cầu)

- `dto/CourseAverageScoreResponse.java`: record `(courseId, courseName,
  averageScore)`.
- `repository/CourseRepository.findAverageScoreByCourseId`: JPQL dùng
  constructor expression (`SELECT new ...CourseAverageScoreResponse(...)`),
  `FROM Course c LEFT JOIN Enrollment e ON e.course = c` — **theta-join**
  tường minh giữa 2 entity không có quan hệ 2 chiều (`Course` không có
  `@OneToMany` ngược lại `Enrollment`). Hợp lệ theo JPA 2.1+ nhưng **chưa
  verify chạy thật với Hibernate 6/Spring Boot 4** — nếu Hibernate báo lỗi
  parse JPQL lúc bootstrap, đây là chỗ đầu tiên cần xem lại.
- `repository/StudentRepository.findStudentsWithoutCourse`: JPQL `NOT EXISTS
  (SELECT 1 FROM Enrollment e WHERE e.student = s)` — so sánh entity-to-entity
  trong subquery, cũng chưa verify chạy thật.
- `service/CourseService.getAverageScore`, `service/StudentService.getWithoutCourse`:
  theo đúng pattern `getById` hiện có (ném `NotFoundException` nếu rỗng/không
  tồn tại course).
- Endpoint mới: `GET /api/courses/{id}/average-score`, `GET
  /api/students/without-course`.

## Native query — top 3 điểm cao nhất mỗi khóa, so điểm với trung bình khóa (AI viết, theo yêu cầu)

JPQL (chuẩn JPA) không hỗ trợ window function (`ROW_NUMBER()`, `RANK()`,
`AVG(...) OVER (...)`) — đây là lý do bài tập này bắt buộc dùng
`@Query(nativeQuery = true)`.

- `dto/TopScoreResponse.java` `(studentId, studentName, score, rankNo)`,
  `dto/ScoreVsAverageResponse.java` `(studentId, studentName, score,
  avgScore, diff)`.
- `repository/TopScoreProjection.java`, `repository/ScoreVsAverageProjection.java`:
  interface projection cho native query — Spring Data map cột trả về (alias
  `student_id`, `student_name`...) theo tên getter (`getStudentId()`...) bằng
  "relaxed binding". **Chưa verify chạy thật** — đây là phần rủi ro nhất vì
  không như JPQL constructor expression (ổn định hơn), native query +
  interface projection phụ thuộc đúng tên alias cột khớp tên getter.
- `repository/CourseRepository.findTop3ScoresByCourseId`: subquery
  `ROW_NUMBER() OVER (ORDER BY e.score DESC)` lọc theo `course_id`, bọc
  subquery rồi `WHERE rank_no <= 3`.
- `repository/CourseRepository.findScoreVsAverageByCourseId`: `AVG(e.score)
  OVER ()` không `PARTITION BY` (vì đã lọc sẵn 1 `course_id` qua `WHERE`, toàn
  bộ dòng kết quả vốn cùng 1 khóa — tương đương `PARTITION BY course_id` khi
  chỉ còn 1 khóa trong tập kết quả).
- `service/CourseService.getTopScores`, `getScoreVsAverage`: check
  `existsById` trước (404 nếu không có khóa học), map `List<Projection>` →
  `List<DTO record>` thủ công (không dùng MapStruct vì mapping 1-dòng, projection
  interface không phải entity).
- Endpoint mới: `GET /api/courses/{id}/top-scores`, `GET
  /api/courses/{id}/score-vs-average`.

## Test (AI viết, theo yêu cầu) — `@WebMvcTest`

- `controller/CourseControllerTest.java` (file mới, 8 test): `getAverageScore`
  (200 có điểm, 200 khi `averageScore = null`, 404), `getTopScores` (200 danh
  sách, 200 danh sách rỗng, 404), `getScoreVsAverage` (200 có chênh lệch
  dương/âm, 404). Chỉ test 3 endpoint mới thêm buổi này — CRUD có sẵn của
  `CourseController` (POST/PUT/DELETE) **chưa có test từ trước**, không thuộc
  phạm vi buổi này.
- `controller/StudentControllerTest.java` (bổ sung 2 test):
  `getStudentsWithoutCourse` (200 danh sách, 200 danh sách rỗng).
- Checklist dùng: happy path + biến thể dữ liệu đặc biệt (`null`/rỗng) + 404.
  Không có case 400 (không `@Valid`/request body) hay 409 (không phải thao
  tác ghi).

## Build & test

- `mvn compile`: sạch, không lỗi (chỉ warning Lombok/`sun.misc.Unsafe` không
  liên quan).
- `mvn test -Dtest=CourseControllerTest,StudentControllerTest`: 25/25 pass,
  `BUILD SUCCESS`. Lần chạy đầu tiên bị `NoClassDefFoundError:
  CourseService` khi dùng `mvn -o test` (offline, không `clean`) — không phải
  lỗi code, do classpath/incremental compile bị stale; chạy lại sau `mvn -o
  clean test-compile` thì hết.
- **Chưa chạy** `StudentApiApplicationTests.contextLoads` (cần Postgres thật)
  và chưa gọi thử endpoint qua app chạy thật trong phiên này.

## Người học tự làm

- Chạy app thật (`spring-boot:run`, profile `dev`, Postgres container lên
  sẵn), gọi thử cả 4 endpoint mới (`average-score`, `top-scores`,
  `score-vs-average`, `without-course`) để xác nhận:
  1. JPQL theta-join (`LEFT JOIN Enrollment e ON e.course = c`) và `NOT EXISTS`
     entity-to-entity không bị Hibernate báo lỗi parse lúc bootstrap.
  2. Native query + interface projection map đúng alias cột, không bị lệch
     tên hay `null` sai chỗ.
  3. `AVG(e.score) OVER ()` không `PARTITION BY` trong
     `score-vs-average` cho ra cùng con số với `average-score` (GROUP BY) trên
     cùng 1 khóa học.
- Tự chạy Phần 1-3 của `sql/week-5/14-jpql-query.sql` trong DBeaver (xem mục
  DB song song ở trên), đặc biệt tự tạo dữ liệu đồng điểm để thấy rõ khác biệt
  `ROW_NUMBER()` vs `RANK()`.
- Nếu phát hiện lỗi ở các điểm "chưa verify" trên, ghi bổ sung vào file này
  (không sửa đè — thêm mục mới ghi rõ lỗi gì, sửa thế nào).
