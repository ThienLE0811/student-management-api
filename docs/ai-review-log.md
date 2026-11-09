# Nhật ký review AI

Ghi lại các lần AI hỗ trợ trong quá trình tự học: AI làm gì, không làm gì, và mình tự làm gì.

## Buổi 9 — Thứ Hai 16/11/2026 — Validation

- AI thêm dependency `spring-boot-starter-validation` vào `student-api/pom.xml` (theo yêu cầu).
- AI tạo file nhật ký này (theo yêu cầu).
- AI **không** viết code validate DTO/Controller — phần này tự làm, AI chỉ review sau khi code xong.

### Cập nhật: AI thêm validation (theo yêu cầu)

AI đã thêm vào `StudentRequest`:
- `fullName`: `@NotBlank`, `@Size(max = 100)`
- `email`: `@NotBlank`, `@Size(max = 100)`, `@Email`
- `birthDate`: `@Past`

Và `@Valid` ở `createStudent` + `updateStudent` trong `StudentController`.

**Đối chiếu với checklist tự liệt kê trước đó — case AI bỏ sót:**
- **Email trùng (unique)**: AI không xử lý — đây là business rule / ràng buộc DB, `@Valid` không bắt được. Vẫn cần tự xử lý riêng ở Service/exception handler (DB hiện chưa có UNIQUE constraint cho `email`, cần thêm nếu muốn check ở tầng DB).
- **Sai định dạng ngày sinh** (`"15/03/2004"`, `"2024-02-30"`): AI không xử lý vì đây là lỗi Jackson deserialize JSON → `LocalDate`, xảy ra **trước** khi `@Valid` chạy, không annotation nào trong Bean Validation bắt được. Cần xử lý bằng `@ExceptionHandler(HttpMessageNotReadableException.class)` ở bài sau.
- **Khoảng trắng đầu/cuối** (`"  Nguyễn Văn An  "`): `@NotBlank` không tự trim dữ liệu lưu xuống DB, chỉ kiểm tra không toàn khoảng trắng. Muốn trim thật thì phải tự xử lý thêm (không phải phạm vi `@Valid`).
- Toàn bộ 3 case trên đã được nói rõ khi review checklist, AI chỉ làm phần "Bean Validation bắt được" theo đúng yêu cầu, chưa đụng tới phần còn lại.
- Các lỗi do @Valid bắt được (fullName rỗng, email sai định dạng, birthDate tương lai)
  đều ném ra cùng 1 LOẠI exception: MethodArgumentNotValidException.
  Vì chưa có @ControllerAdvice xử lý riêng, Spring dùng handler mặc định
  -> response trả về client luôn là "400 Bad Request" chung chung,
  không có thông tin field nào sai / sai gì.
  => Cần @ControllerAdvice + @ExceptionHandler để lấy chi tiết lỗi từ
  BindingResult và trả JSON rõ ràng theo từng field.
