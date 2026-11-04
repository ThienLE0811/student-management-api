# Student Management API

API quản lý sinh viên (Student Management) được xây dựng bằng Spring Boot, cung cấp các chức năng CRUD cơ bản cho sinh viên, lớp học, khóa học... thông qua REST API.

> ⚠️ Dự án hiện đang ở giai đoạn khởi tạo, chưa có mã nguồn. README này đóng vai trò là khung sườn (template) để định hướng phát triển; các mục sẽ được cập nhật chi tiết khi code được triển khai.

## Mục tiêu

- Xây dựng REST API quản lý thông tin sinh viên (CRUD).
- Thực hành kiến trúc layered (Controller → Service → Repository → Database) với Spring Boot.
- Thực hành thiết kế và tương tác cơ sở dữ liệu quan hệ (PostgreSQL/Oracle) qua Spring Data JPA.

## Tech Stack

- **Ngôn ngữ**: Java 25
- **Framework**: Spring Boot, Spring MVC
- **Data access**: Spring Data JPA
- **Database**: PostgreSQL / Oracle (tùy môi trường)
- **Build tool**: Maven hoặc Gradle (sẽ chốt khi khởi tạo project)
- **API**: RESTful API

## Kiến trúc dự kiến

```
Controller  →  Service  →  Repository  →  Database
```

- **Controller**: Tiếp nhận HTTP request, validate đầu vào, trả về response.
- **Service**: Xử lý logic nghiệp vụ, quản lý transaction boundary.
- **Repository**: Giao tiếp với database thông qua Spring Data JPA.
- **Entity/Model**: Ánh xạ bảng dữ liệu (sinh viên, lớp học, khóa học...).

## Cấu trúc thư mục dự kiến

```
student-management-api/
├── src/
│   ├── main/
│   │   ├── java/com/example/studentmanagement/
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── dto/
│   │   │   └── exception/
│   │   └── resources/
│   │       └── application.properties (hoặc application.yml)
│   └── test/
├── pom.xml (hoặc build.gradle)
└── README.md
```

## Yêu cầu môi trường

- JDK 25
- Maven hoặc Gradle
- PostgreSQL hoặc Oracle Database
- IDE: IntelliJ IDEA (khuyến nghị)

## Cài đặt & chạy dự án

```bash
# Clone dự án
git clone <repository-url>
cd student-management-api

# Build project (Maven)
./mvnw clean install

# Chạy ứng dụng
./mvnw spring-boot:run
```

> Lệnh build/run cụ thể sẽ được cập nhật sau khi project được khởi tạo bằng Spring Initializr.

## Cấu hình database

Cấu hình kết nối database sẽ được khai báo trong `application.yml`, ví dụ:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/student_management
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

## API Endpoints (dự kiến)

| Method | Endpoint            | Mô tả                      |
|--------|---------------------|----------------------------|
| GET    | /api/students       | Lấy danh sách sinh viên    |
| GET    | /api/students/{id}  | Lấy thông tin 1 sinh viên  |
| POST   | /api/students       | Tạo mới sinh viên          |
| PUT    | /api/students/{id}  | Cập nhật thông tin sinh viên |
| DELETE | /api/students/{id}  | Xóa sinh viên              |

## Roadmap

Dự án được phát triển theo lộ trình học Spring Boot & Database, bao gồm các giai đoạn: thiết kế entity, CRUD cơ bản, validation & exception handling, quan hệ giữa các bảng, phân trang/tìm kiếm, bảo mật (Spring Security/JWT) và triển khai.

## Quy tắc trong tháng

- **Quy tắc vàng**: không commit dòng code nào mà bạn không giải thích được. Khi AI viết, hỏi lại "vì sao" cho đến khi hiểu, hoặc xóa đi tự viết.
- **Tuần 3**: không mở AI để viết code. Kẹt quá 30 phút thì chỉ nhờ AI giải thích thông báo lỗi, không xin code sửa sẵn.
- **Nhật ký review AI** (từ tuần 4): mỗi lần AI viết sai (N+1, quên `@Transactional`, nuốt exception, dùng API cũ của Spring Boot 3…) thì ghi một dòng. Mục tiêu cuối tháng: ít nhất 10 lỗi.
- **DB song song**: mỗi buổi 45–60 phút, tự gõ, không AI (trừ việc sinh dữ liệu mẫu). Chi tiết ở bảng "DB song song" dưới mỗi tuần.
- **30 phút cuối buổi thứ Sáu**: trả lời câu hỏi tự kiểm tra của tuần, cập nhật nhật ký, đẩy code lên GitHub và gắn tag `week-N`.

## Đóng góp

Đây là dự án cá nhân phục vụ mục đích học tập/thực hành. Các issue và pull request đóng góp (nếu có) sẽ được xem xét trực tiếp.
