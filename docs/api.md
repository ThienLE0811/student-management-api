# API — Quản lý sinh viên (tự thiết kế, buổi 1)

Tự điền trước, điền xong mới mở bảng "Student API lớn dần" trong lộ trình để đối chiếu.

| # | Method | URL                | Request                                          | Response thành công         | Status thành công | Status lỗi (trường hợp) | SQL tương ứng |
|---|--------|--------------------|--------------------------------------------------|-----------------------------|-------------------|----------------------|---------------|
| 1 | GET    | /api/students      |                                                  | danh sách sinh viên         | 200               | 500 (lỗi server) | `SELECT * FROM students WHERE full_name ILIKE ... ORDER BY id;` (lọc theo `minAge`/`maxAge` xử lý ở tầng Java, không nằm trong câu SQL) |
| 2 | GET    | /api/students/{id} |                                                  | chi tiết sinh viên          | 200               | 404 (không tìm thấy id) | `SELECT * FROM students WHERE id = ...;` |
| 3 | POST   | /api/students      | {fullName, email, birthDate} và có thể nhiều hơn | thông tin sinh viên đã thêm | 201               | 400 (dữ liệu sai/thiếu field), 409 (email đã tồn tại) | `INSERT INTO students (full_name, email, birth_date, created_at) VALUES (..., ..., ..., ...);` |
| 4 | PUT    | /api/students/{id} | {fullName, email, birthDate} và có thể nhiều hơn | thông tin sinh viên đã sửa  | 200               | 400 (dữ liệu sai/thiếu field), 404 (không tìm thấy id), 409 (email đã tồn tại ở sinh viên khác) | `UPDATE students SET full_name = ..., email = ..., birth_date = ... WHERE id = ...;` |
| 5 | DELETE | /api/students/{id} |                                                  | không có nội dung           | 204               | 404 (không tìm thấy id) | `DELETE FROM students WHERE id = ...;` |

## Đối chiếu với "Student API lớn dần" (điền sau khi đã tự làm xong bảng trên)

- Giống:
- Khác (và vì sao bảng tham khảo làm vậy):