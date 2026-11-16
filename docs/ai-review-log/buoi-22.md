# Buổi 22 — Ghi đồng thời: optimistic locking (phần 1 — tạo bug)

Mục tiêu buổi: hiểu lost update từ gốc (DB) rồi tái hiện đúng y hệt bug đó
ở tầng ứng dụng, trước khi sửa bằng optimistic locking (`@Version`) ở phần
sau.

## Phần DB (override theo yêu cầu người học: "bạn làm luôn, tôi review")

Khác mặc định "DB song song" (AI chỉ sinh dữ liệu mẫu) — người học chủ động
yêu cầu AI tự chạy cả 2 kịch bản SQL thật trên Postgres dev, dán nguyên kết
quả thật.

### Tái hiện lost update bằng 2 session `psql` song song

Vì `classes` không có sẵn khái niệm "chỗ trống", AI tạo 1 bảng tạm
`demo_slot(id, remaining_slots)` chỉ để demo (không đụng schema thật), seed
`remaining_slots = 1`. Mô phỏng 2 session bằng 2 tiến trình `psql -f` chạy
nền, chọn thời điểm bằng `pg_sleep` để ép đúng thứ tự xen kẽ:

- S1: `BEGIN` → đọc `remaining_slots=1` → sleep 2s → `UPDATE ... = 0` →
  `COMMIT` (ở t=2).
- S2: `BEGIN` → sleep 0.5s → đọc `remaining_slots=1` (stale, vì S1 chưa
  commit) → sleep tiếp → ở t=3 (sau khi S1 đã commit) vẫn `UPDATE ... = 0`
  dựa trên giá trị đã đọc trước đó → `COMMIT`.

Kết quả thật: `remaining_slots` cuối cùng = 0, dù có 2 lượt "ghi danh" xảy
ra — update của S1 bị mất (lost update), không có lỗi nào được báo ra.

### Sửa bằng `SELECT ... FOR UPDATE`

Lặp lại với `FOR UPDATE`: S2 gọi lúc t≈0.5 (`clock_timestamp` =
`10:07:15.029`) nhưng bị **block thật sự** tới khi S1 commit (giải phóng
lock) — S2 chỉ chạy tiếp lúc `10:07:15.437` (~0.4s sau), và lúc đó đọc được
giá trị **mới nhất** (`0`, không còn stale `1`). Đã dọn sạch bảng tạm +
script sau khi demo xong.

## Phần Java — thêm capacity cho lớp (chưa chống race, cố ý để dành phần sau)

### Quyết định phạm vi (hỏi người học trước khi code)

- `POST /api/classes/{id}/students`: **gán sinh viên đã tồn tại** vào lớp
  (không phải tạo mới) — tái dùng cơ chế `class_transfer_history` đã có từ
  buổi 20.
- `enrolledCount` chỉ được cập nhật ở đúng endpoint mới này. Các endpoint cũ
  (`POST/PUT /api/students`, `PUT /api/students/{id}/class/{classId}`,
  `DELETE /api/students/{id}`) **chưa đụng tới** — nghĩa là `enrolledCount`
  có thể lệch nếu dùng các endpoint đó để đổi lớp. Đây là nợ kỹ thuật có chủ
  đích cho phạm vi buổi này, không phải AI bỏ sót.

### Code (AI viết, theo yêu cầu)

- `db/migration/V4__class_capacity.sql`: thêm `capacity`, `enrolled_count`
  vào `classes` (có `DEFAULT` khi `ADD COLUMN` để không fail trên dữ liệu
  dev cũ), backfill `enrolled_count` theo đúng số sinh viên thực tế mỗi
  lớp, đảm bảo `capacity >= enrolled_count` cho lớp cũ rồi mới `DROP
  DEFAULT`.
- `entity/SchoolClass.java`: thêm field `capacity`, `enrolledCount`; đổi
  constructor thêm 2 tham số này (sửa theo 3 chỗ test cũ đang gọi
  constructor 3 tham số).
- `dto/ClassRequest.java` (+`capacity`, `@NotNull @Positive`),
  `dto/ClassResponse.java` (+`capacity`, `enrolledCount`) —
  `ClassMapper` không cần sửa vì MapStruct tự map theo tên field.
- `dto/AddStudentToClassRequest.java` (mới): `record(Long studentId)`.
- `exception/ClassFullException.java` (mới) + handler trong
  `GlobalExceptionHandler` → `409 CONFLICT`.
- `service/StudentService.addToClass(classId, studentId)`: 404 nếu lớp/sinh
  viên không tồn tại; no-op nếu sinh viên đã ở đúng lớp đó; **check
  `enrolledCount >= capacity` rồi mới ghi** (đây chính là điểm hở race —
  check-then-act không có lock); nếu còn chỗ thì chuyển lớp, +1 lớp mới/-1
  lớp cũ, ghi `ClassTransferHistory`.
- `controller/ClassController.addStudentToClass`: `POST
  /api/classes/{id}/students`.
- Sửa 3 test cũ cho khớp constructor/record mới (`StudentRepositoryTest`,
  `EnrollmentRepositoryTest`, `ClassControllerTest`) — chỉ để compile, không
  đổi logic.

### Test mới cho endpoint (`ClassControllerTest`, `@WebMvcTest`)

Checklist đối chiếu trước khi viết (theo đúng quy ước): 200 (còn chỗ), 404
(lớp không tồn tại), 404 (sinh viên không tồn tại), 409 (lớp đầy), 400
(thiếu `studentId`). Cả 5 case đều có test, `mvn test -Dtest=ClassControllerTest`
pass.

### Kiểm thử thủ công qua app thật (profile `dev`)

Tạo lớp `capacity=1`, gọi `POST .../students` 2 lần tuần tự với 2 sinh viên
khác nhau: lần 1 → 200 (`enrolledCount=1`), lần 2 → 409 đúng message "Lớp đã
hết chỗ". Verify thêm 404 (lớp/sinh viên không tồn tại) và 400 (thiếu
`studentId`) — đúng như kỳ vọng. Dữ liệu test đã dọn sạch khỏi DB dev sau
khi xong (kể cả dòng `class_transfer_history` phát sinh, vì `DELETE
/api/classes/{id}` bị lỗi 500 do FK — hành vi cũ đã có từ trước, không phải
bug mới, không sửa trong phạm vi buổi này).

## Tái hiện race condition ở tầng Java (`ExecutorService` + `CountDownLatch`)

File mới: `src/test/java/.../service/StudentServiceConcurrencyTest.java`
(`@SpringBootTest` + Testcontainers Postgres riêng, **không** đụng DB dev —
Flyway tự chạy V1-V4 trên container rỗng giống hệt production).

Kịch bản: 1 lớp `capacity=1`, 2 sinh viên đang ở lớp khác. 2 thread cùng gọi
`addToClass(lớp đích, sinh viên)` , đồng bộ thời điểm bắt đầu bằng
`CountDownLatch` để cả 2 cùng xuất phát.

### Kết quả thật — khác giả định ban đầu của AI

Giả định ban đầu: `enrolledCount` sẽ **vượt** `capacity` (vd lên 2). Chạy
thật thì không phải vậy — cả 2 request đều **thành công** (không ai bị
`409`), cả 2 sinh viên đều thực sự được chuyển vào lớp (`actualHeadcount =
2`), nhưng `enrolledCount` (counter) cuối cùng chỉ còn **1** vì write sau
ghi đè lên write trước — lost update kinh điển, y hệt kịch bản đã làm ở
phần DB. Tức là bug thật **không nằm ở chỗ counter sai** mà ở chỗ
**capacity bị vi phạm trên thực tế** (`actualHeadcount(2) > capacity(1)`)
ngay cả khi counter "trông có vẻ hợp lệ". Đã sửa lại assertion của test cho
đúng với quan sát thật thay vì ép theo giả định ban đầu:

```java
long actualHeadcount = studentRepository.findBySchoolClassId(targetClass.getId()).size();
assertThat(actualHeadcount).isGreaterThan(reloaded.getCapacity());
```

Chạy lại 2 lần liên tiếp đều tái hiện đúng (`capacity=1, enrolledCount=1,
actualHeadcount=2`) — test này **cố ý pass vì đang ghi nhận đúng hành vi
buggy hiện tại**, không phải hành vi mong muốn cuối cùng. Sẽ fix ở phần
optimistic locking (`@Version`) tiếp theo của buổi học, lúc đó test này kỳ
vọng sẽ đổi sang: 1 trong 2 request phải thất bại rõ ràng (409 hoặc lỗi
optimistic lock), và `actualHeadcount` không bao giờ vượt `capacity`.

## Build & test (trước khi fix)

- `mvn -o compile` + `mvn -o test-compile`: sạch.
- `mvn -o test` (toàn bộ suite, 62 test kể cả `StudentServiceConcurrencyTest`
  mới): **62/62 pass**, không regression.

## Fix bằng optimistic locking (`@Version`)

### Code (AI viết, theo yêu cầu)

- `db/migration/V5__class_version.sql`: thêm cột `version` (`BIGINT NOT
  NULL`, có `DEFAULT 0` lúc `ADD COLUMN` để không fail trên dữ liệu dev cũ,
  rồi `DROP DEFAULT`).
- `entity/SchoolClass.java`: thêm field `@Version private Long version;` —
  không đưa vào constructor (JPA tự quản lý, tự khởi tạo 0 khi insert).
- `exception/GlobalExceptionHandler.java`: thêm
  `@ExceptionHandler(ObjectOptimisticLockingFailureException.class)` → `409
  CONFLICT`. **Không** sửa `StudentService.addToClass` — exception này tự
  ném ra khi transaction commit (lúc flush ở cuối proxy `@Transactional`),
  tự propagate lên tới `GlobalExceptionHandler` mà không cần try/catch thủ
  công trong service, đúng theo yêu cầu "bắt
  `ObjectOptimisticLockingFailureException` → 409" ở tầng kiến trúc đã quy
  định (mọi lỗi đi qua `GlobalExceptionHandler`, không catch rải rác).
- Thêm 1 test case `addStudentToClass_tra_ve_409_khi_co_xung_dot_optimistic_lock`
  trong `ClassControllerTest` (mock service ném thẳng
  `ObjectOptimisticLockingFailureException`, verify controller trả đúng
  409).

### Sửa lại `StudentServiceConcurrencyTest` cho đúng kỳ vọng mới

Đổi từ "ghi nhận bug" sang "verify fix": 2 thread cùng gọi `addToClass` vào
lớp `capacity=1`, đếm số request thành công/bị optimistic-lock-conflict,
rồi so `enrolledCount` + `actualHeadcount` với `capacity`.

```java
assertThat(successCount.get()).isEqualTo(1);
assertThat(conflictCount.get()).isEqualTo(1);
assertThat(actualHeadcount).isEqualTo(reloaded.getCapacity());
assertThat(reloaded.getEnrolledCount()).isEqualTo(reloaded.getCapacity());
```

Gặp 1 lỗi nhỏ lúc viết: so sánh `long` (từ `List.size()` đã lỡ gán qua
`long`) với `Integer` (`getCapacity()`) bằng AssertJ `isEqualTo` bị strict
type mismatch (`expected: 1, but was: 1L`) dù giá trị bằng nhau — sửa bằng
cách khai `actualHeadcount` đúng kiểu `int` (khớp kiểu trả về thật của
`List.size()`) thay vì `long`.

### Kết quả thật — chạy 4 lần liên tiếp đều đúng

```
[race] success=1, conflict=1, capacity=1, enrolledCount=1, actualHeadcount=1
```

Đúng như kỳ vọng: luôn đúng 1 request thành công, 1 request bị
`ObjectOptimisticLockingFailureException`, `enrolledCount` và
`actualHeadcount` không bao giờ vượt `capacity` nữa.

### Verify qua app thật (profile `dev`) bằng 2 `curl` chạy song song

Tạo lớp `capacity=1` + 2 sinh viên ở lớp khác, bắn 2 `POST
/api/classes/{id}/students` song song (chạy nền `&` + `wait`):

- Request 1: `200`, sinh viên được chuyển vào lớp.
- Request 2: `409` — `{"detail":"Lớp vừa bị cập nhật bởi request khác, vui
  lòng thử lại", ...}`.
- Trạng thái cuối: `enrolledCount=1`, đúng 1 sinh viên trong lớp — khớp
  `capacity=1`.

Dữ liệu test đã dọn sạch khỏi DB dev sau khi verify xong.

## (Tùy chọn) So sánh với `@Lock(PESSIMISTIC_WRITE)`

### Code (AI viết, theo yêu cầu)

- `repository/ClassRepository.findByIdForUpdate(Long id)`: thêm
  `@Lock(LockModeType.PESSIMISTIC_WRITE)` + `@Query` riêng (không override
  `findById` có sẵn của `JpaRepository`, vì `@Lock` không gắn được trực
  tiếp lên method kế thừa).
- `service/StudentService.addToClassPessimistic(classId, studentId)`:
  method **riêng**, song song với `addToClass` (không thay thế) — khóa cả
  2 lớp (`oldClass`, `newClass`) ngay lúc đọc bằng `findByIdForUpdate`,
  theo **thứ tự id tăng dần** để tránh deadlock khi 2 request khóa chéo 2
  lớp ngược chiều nhau. **Không** thêm endpoint REST mới cho method này —
  phạm vi tùy chọn này chỉ để so sánh cơ chế qua test, chưa phải tính năng
  sản phẩm.

### Khác biệt quan sát được so với optimistic (`@Version`)

Thêm 1 test case `addToClassPessimistic_chay_song_song_chi_1_request_thanh_cong_nho_khoa_dong`
trong cùng file `StudentServiceConcurrencyTest`, cùng kịch bản 2 thread +
`CountDownLatch`. Kết quả thật, chạy 4 lần liên tiếp:

```
[race]             success=1, conflict=1,  capacity=1, enrolledCount=1, actualHeadcount=1
[race-pessimistic] success=1, classFull=1,  capacity=1, enrolledCount=1, actualHeadcount=1
```

Cả 2 cách đều chặn đúng (không vượt `capacity`), nhưng **khác nhau ở loại
lỗi** request thua cuộc nhận được:

| | Optimistic (`@Version`) | Pessimistic (`@Lock(PESSIMISTIC_WRITE)`) |
|---|---|---|
| Thời điểm phát hiện xung đột | Lúc **commit** (flush cuối transaction) | Lúc **đọc** (`SELECT ... FOR UPDATE` tự block) |
| Request thua cuộc bị gì | `ObjectOptimisticLockingFailureException` — lỗi hạ tầng (infra), phải map thủ công sang 409 ở `GlobalExceptionHandler` | `ClassFullException` — lỗi nghiệp vụ bình thường, vì sau khi được nhả khóa, request đọc lại đúng `enrolledCount` mới nhất và tự nhiên rơi vào nhánh "hết chỗ" đã có sẵn |
| Có chờ (block) không | Không — cả 2 transaction chạy song song, request sau chỉ biết thua lúc commit | Có — request đến sau phải **chờ** ở mức DB tới khi request trước commit/rollback xong |
| Throughput khi tranh chấp cao | Tốt hơn (không block, nhưng tốn công retry/thất bại) | Kém hơn (nghẽn tại điểm khóa, nhưng không cần retry) |

Bài học rút ra: optimistic phù hợp khi tranh chấp **hiếm** (đa số request
không đụng nhau, phí tổn retry thấp); pessimistic phù hợp khi tranh chấp
**thường xuyên** trên cùng 1 dòng (chấp nhận chờ để tránh request phải
thất bại rồi tự retry ở tầng client).

## Build & test (sau khi fix + so sánh)

- `mvn -o compile` + `mvn -o test-compile`: sạch.
- `mvn -o test -Dtest=StudentServiceConcurrencyTest`: chạy riêng 4 lần liên
  tiếp, cả 2 test case đều pass, kết quả ổn định như bảng trên.
- `mvn -o test` (toàn bộ suite): **63/63 pass**, không regression.
- Gặp 1 lỗi nhỏ khi thêm test case thứ 2: `setUp()` dùng tên lớp/email cố
  định ("Lop nguon", "race-a@test.com"...) → 2 test case trong cùng file
  chạy trên cùng 1 Testcontainers container (dữ liệu không tự rollback
  giữa các test) → vi phạm `uk_classes_name`/`uk_students_email` ở lần
  chạy thứ 2. Sửa bằng cách thêm hậu tố `System.nanoTime()` vào tên/email
  mỗi lần `setUp()` chạy.

## Người học tự làm / chưa làm

- Chưa đồng bộ `enrolledCount` ở 4 chỗ còn lại (xem phần "Quyết định phạm
  vi" ở trên) — nếu muốn `enrolledCount` luôn đúng tuyệt đối thì đây là việc
  cần làm thêm, ngoài phạm vi đã thống nhất.
- `addToClassPessimistic` chỉ tồn tại ở tầng Service + test, chưa có endpoint
  REST riêng — nếu muốn thử qua Postman/curl thật thì cần tự thêm route
  hoặc tạm đổi `ClassController` gọi sang method này.
