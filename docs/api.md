# API — Quản lý sinh viên (tự thiết kế, buổi 1)

Tự điền trước, điền xong mới mở bảng "Student API lớn dần" trong lộ trình để đối chiếu.

| # | Method | URL                | Request                                          | Response thành công         | Status thành công | Status lỗi (trường hợp) |
|---|--------|--------------------|--------------------------------------------------|-----------------------------|-------------------|----------------------|
| 1 | GET    | /api/students      |                                                  | danh sách sinh viên         | 200               | 500 (lỗi server) |
| 2 | GET    | /api/students/{id} |                                                  | chi tiết sinh viên          | 200               | 404 (không tìm thấy id) |
| 3 | POST   | /api/students      | {fullName, email, birthDate} và có thể nhiều hơn | thông tin sinh viên đã thêm | 201               | 400 (dữ liệu sai/thiếu field), 409 (email đã tồn tại) |
| 4 | PUT    | /api/students/{id} | {fullName, email, birthDate} và có thể nhiều hơn | thông tin sinh viên đã sửa  | 200               | 400 (dữ liệu sai/thiếu field), 404 (không tìm thấy id), 409 (email đã tồn tại ở sinh viên khác) |
| 5 | DELETE | /api/students/{id} |                                                  | không có nội dung           | 204               | 404 (không tìm thấy id) |

## Đối chiếu với "Student API lớn dần" (điền sau khi đã tự làm xong bảng trên)

- Giống:
- Khác (và vì sao bảng tham khảo làm vậy):
