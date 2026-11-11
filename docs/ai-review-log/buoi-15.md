# Buổi 15 — Thứ Ba 24/11/2026 — @ManyToOne, @OneToMany (Student – SchoolClass)

- Tự vẽ ERD trước khi code: `CLASSES (1) --- has ---< STUDENTS (N)`, FK `class_id`
  nằm ở `STUDENTS`, chốt 3 điểm trước khi AI đụng code: tên cột `birth_date`
  (không phải `date_of_birth` như bản nháp ERD ban đầu), migrate `SchoolClass`
  sang JPA thật (không chỉ thêm annotation lên record), `class_id` là `NOT NULL`
  (sinh viên bắt buộc thuộc 1 lớp).
- Thống nhất dùng tên `SchoolClass` (không đổi thành `ClassRoom` dù tài liệu gốc
  dùng tên đó) — đúng quy ước đặt tên đã có trong `CLAUDE.md` từ buổi 13, tránh
  rename lan man không cần thiết (endpoint `/api/classes`, `ClassService`,
  `ClassRepository`... đã tồn tại từ trước).

## Migrate `SchoolClass` từ record in-memory sang JPA Entity thật

- `entity/SchoolClass.java`: `record` → `@Entity` class (giống pattern
  `Student` ở buổi 14), thêm cột `created_at`, bỏ `@AllArgsConstructor` của
  Lombok (viết tay constructor 3 tham số `id, name, created_at` để không vướng
  field `students` mới thêm).
- `repository/ClassRepository.java`: bỏ hẳn `LinkedHashMap` + `AtomicLong`,
  chuyển `extends JpaRepository<SchoolClass, Long>`, thêm derived query
  `existsByNameIgnoreCase` / `existsByNameIgnoreCaseAndIdNot` thay cho vòng lặp
  check trùng tên thủ công.
- `service/ClassService.java`: đổi toàn bộ logic sang dùng JPA
  (`existsById`, `findById`, `save`) thay vì thao tác `Map`.

## Quan hệ 1-N `Student` ↔ `SchoolClass`

- `Student.schoolClass`: `@ManyToOne(fetch = FetchType.LAZY)` +
  `@JoinColumn(name = "class_id", nullable = false)` — owning side, giữ FK.
- `SchoolClass.students`: `@OneToMany(mappedBy = "schoolClass", fetch = LAZY)`
  — inverse side, chỉ để navigate ngược.
- `StudentRequest` thêm `@NotNull Long classId`; `StudentResponse` thêm
  `Long classId` (**không** kèm `className` để tránh N+1 query khi lazy-load —
  `classId` lấy trực tiếp từ `schoolClass.getId()`, proxy Hibernate trả về id
  mà không cần query DB thêm).
- `StudentService.create/update`: tra `SchoolClass` theo `classId` qua
  `ClassRepository`, ném `NotFoundException` nếu `classId` không tồn tại.
- Endpoint mới `GET /api/classes/{id}/students` (`ClassController` gọi
  `StudentService.getByClassId`) — trả `List<StudentResponse>` (DTO), không
  trả Entity trực tiếp để tránh vòng lặp JSON `Student → SchoolClass →
  students → ...`.
- `StudentRepository` thêm `findBySchoolClassId` (`@Query` JPQL, vì không có
  property `schoolClassId` phẳng mà phải traverse `s.schoolClass.id`).

## Regression AI phát hiện và tự sửa

- `ClassControllerTest` fail nếu không mock thêm `StudentService` — vì
  `ClassController` giờ có thêm dependency (gọi sang `StudentService` cho
  endpoint `/students`). Đã thêm `@MockitoBean StudentService` + 2 test case
  (happy path, 404 khi lớp không tồn tại).
- `StudentControllerTest`: toàn bộ chỗ dựng `StudentRequest`/`StudentResponse`
  phải thêm tham số `classId` (đổi API contract). Thêm test case mới
  `createStudent_tra_ve_400_khi_thieu_classId` (`@NotNull` validation).

## Sự cố thực tế khi chạy test: thêm cột NOT NULL vào bảng đã có dữ liệu

- `mvn test` (chạy `StudentApiApplicationTests`, context load thật với
  Postgres dev) fail: `column "class_id" of relation "students" contains null
  values`. Nguyên nhân: bảng `students` trong Postgres dev còn vài dòng dữ
  liệu test cũ từ buổi 14 (không có `class_id`), Postgres từ chối
  `ALTER TABLE ... ADD COLUMN class_id NOT NULL` trực tiếp.
- Không phải lỗi code — `ddl-auto: update` không tự backfill dữ liệu cũ. Theo
  lựa chọn của người học (dữ liệu dev, không cần giữ), AI chạy
  `TRUNCATE TABLE students;` trên container `student-api-postgres` để dọn,
  sau đó Hibernate migrate schema thành công (`ADD COLUMN class_id bigint not
  null` + FK) và test pass (30/30).
- Đã ghi bài học này vào `CLAUDE.md` (mục "Quy tắc chung") để nhắc nhớ cho các
  buổi sau: thêm field bắt buộc vào entity đã có dữ liệu dev luôn cần dọn/
  backfill dữ liệu trước.

## Cập nhật `CLAUDE.md` cho khớp thực tế

- Sửa phần Tech stack: không còn mô tả "chưa dùng JPA/DB thật, repository in-
  memory Map" (đã lỗi thời từ buổi 14) — ghi đúng: toàn bộ `repository/` là
  `JpaRepository`, entity Java là nguồn sự thật cho schema, `sql/` vẫn là
  track học riêng, có thể lệch schema thật nếu không chủ động đối chiếu.
- Sửa mô tả tầng `repository/` trong phần kiến trúc 3 tầng cho khớp.
- Thêm lưu ý `ddl-auto: update` + cột NOT NULL (xem mục trên).

## Thí nghiệm: `CascadeType.ALL` trên `SchoolClass.students` rồi xóa 1 lớp

- Thêm tạm `cascade = CascadeType.ALL` vào `@OneToMany` của `SchoolClass`,
  chạy app thật (`spring-boot:run`, profile `dev`, Postgres thật), tạo 1 lớp +
  1 sinh viên thuộc lớp đó qua API thật, gọi `DELETE /api/classes/{id}`.
- **Quan sát log Hibernate thật:**
  ```
  delete from students where id=?
  delete from classes where id=?
  ```
  `DELETE /api/classes/{id}` vẫn trả `204 No Content` bình thường, nhưng
  Hibernate tự phát sinh thêm `DELETE FROM students` trước khi xóa lớp.
  `GET /api/students/{id}` sau đó trả `404` — sinh viên đã bị xóa theo, dù
  request chỉ yêu cầu xóa lớp.
- **Root cause:** cascade `ALL` bao gồm `REMOVE`; vì `Student.schoolClass` là
  `nullable = false`, Hibernate không thể set `class_id = null` để "mồ côi
  hóa" sinh viên khi xóa lớp cha → cách duy nhất giữ đúng ràng buộc NOT NULL
  là xóa luôn sinh viên con.
- **Kết luận (bài học, không phải bug):** `CascadeType.ALL` trên quan hệ
  "lớp học – sinh viên" nguy hiểm trong thực tế vì xóa nhầm 1 lớp sẽ âm thầm
  xóa sạch sinh viên của lớp đó mà API không cảnh báo gì (vẫn 204 bình
  thường). Muốn chặn xóa lớp còn sinh viên, nên validate tường minh ở
  `ClassService.deleteById()` (vd: kiểm tra `findBySchoolClassId` không rỗng
  thì ném exception nghiệp vụ → 409), không dựa vào cascade để "dọn hộ".
- Đã bỏ `cascade = CascadeType.ALL` sau khi ghi nhận, kill các tiến trình app
  chạy thử nghiệm, build + test lại sạch (30/30, `BUILD SUCCESS`).

## Lưu ý cho buổi sau

- `docs/sql/` (track học riêng) chưa được cập nhật để khớp với schema JPA
  mới (`classes` có thêm `created_at`, `students` có thêm `class_id`) — nếu
  buổi sau có đối chiếu 2 track thì cần tự cập nhật tay.
- Việc chặn xóa lớp còn sinh viên (thay vì để cascade xóa) chưa được code —
  mới dừng ở mức ghi nhận bài học từ thí nghiệm, chưa phải yêu cầu của buổi
  này.
