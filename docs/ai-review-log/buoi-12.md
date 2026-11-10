# Buổi 12 — Thứ Năm 19/11/2026 — Test Controller với MockMvc

- AI viết toàn bộ `StudentControllerTest.java` (`@WebMvcTest` + `MockMvc` + mock `StudentService`), theo yêu cầu ở bước này của lộ trình ("AI viết code test, bạn chạy, đọc từng test, sửa").
- Tự liệt kê checklist test case trước khi AI viết, rồi đối chiếu — AI bổ sung thêm vài case tự liệt kê còn thiếu: GET list có query param, GET theo id thành công (200), PUT not-found (404), `@Size` (tên quá dài), và case ghi nhận bug (xem bên dưới). Tự liệt kê ban đầu chỉ tập trung vào nhánh lỗi của POST, bỏ sót happy-path của GET/PUT.

## Lỗi AI mắc phải — dùng API cũ của Spring Boot 3 (không tự phát hiện, phải build mới lộ ra)

Dự án dùng **Spring Boot 4.1.2**, nhưng AI viết code test lần đầu theo quán tính Spring Boot 3:
- Import `com.fasterxml.jackson.databind.ObjectMapper` — sai, vì Spring Boot 4 đổi sang **Jackson 3** (`tools.jackson.databind.ObjectMapper`, package gốc `tools.jackson.*` chứ không còn `com.fasterxml.jackson.databind` cho databind/core).
- Import `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` — sai, package đã đổi thành `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`.
- Nếu dùng `@MockBean` (annotation cũ) sẽ không compile được vì bị gỡ bỏ khỏi Spring Framework/Boot 4 — AI đã dùng đúng `@MockitoBean` ngay từ đầu (không phải lỗi, nhưng đáng để tự kiểm tra lại mỗi lần vì đây là chỗ dễ sai nhất khi AI "nhớ nhầm" theo bản cũ).
=> Bài học: với framework version mới/ít phổ biến, không tin tưởng API "quen thuộc" của AI — phải build thử ngay, lỗi compile sẽ lộ ra ngay lập tức.

## Bug phát hiện qua việc viết test (không phải AI viết sai, mà test lộ ra lỗi có sẵn trong code)

Khi gửi `birthDate` sai định dạng (vd: `"khong-phai-ngay"`), kỳ vọng ban đầu là 400, nhưng thực tế code hiện tại trả về **500**:
- Lỗi xảy ra ở bước Jackson deserialize JSON → `StudentRequest` (ném `HttpMessageNotReadableException`), xảy ra **trước** khi Bean Validation (`@Past`) có cơ hội chạy.
- `GlobalExceptionHandler` chỉ có 4 handler: `NotFoundException`, `DuplicateEmailException`, `MethodArgumentNotValidException`, và catch-all `Exception.class`. `HttpMessageNotReadableException` không khớp 3 handler đầu nên rơi vào catch-all → 500.
- Bình thường Spring Boot tự xử lý `HttpMessageNotReadableException` trả 400, nhưng catch-all `@ExceptionHandler(Exception.class)` có độ ưu tiên cao hơn nên "nuốt mất" hành vi mặc định đó.
- Test `createStudent_sai_dinh_dang_ngay_sinh_hien_tai_tra_ve_500` được viết để **ghi nhận hành vi hiện tại (bug)**, không phải hành vi mong muốn — cần sửa `GlobalExceptionHandler` thêm `@ExceptionHandler(HttpMessageNotReadableException.class)` ở bài sau, lúc đó phải sửa lại test này thành expect 400.
- Đây đúng là case đã dự đoán từ Buổi 9 (xem [buoi-09.md](buoi-09.md)) nhưng chưa xử lý, giờ có test để chứng minh bằng thực nghiệm.
