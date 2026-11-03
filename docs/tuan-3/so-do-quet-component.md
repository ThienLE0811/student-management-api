# Sơ đồ: Spring quét @Component → tạo bean → tiêm qua constructor

Ví dụ dựa trên project `scratch/di-demo-spring`.

```mermaid
flowchart TD
    subgraph "1. Component scan (lúc SpringApplication.run chạy)"
        A["@SpringBootApplication
DiDemoSpringApplication"] -->|quét package com.thienlv.didemospring| B[Tìm thấy class có
@Component / @Service / @Repository]
    end

    subgraph "2. Tạo bean (theo đúng thứ tự phụ thuộc)"
        B --> C["@Repository
StudentRepository
(không phụ thuộc gì → tạo trước)"]
        B --> D["@Service
EmailNotificationService
implements NotificationService"]
        B --> E["@Service
SmsNotificationService
implements NotificationService"]
        C --> F["@Service
StudentService
constructor(StudentRepository)"]
    end

    subgraph "3. Tiêm qua constructor"
        F --> G["@Component
StudentRunner implements CommandLineRunner
constructor(StudentService, StudentRepository, NotificationService)"]
        C --> G
        D -. "2 bean cùng kiểu NotificationService
→ phải @Primary hoặc @Qualifier
để Spring biết chọn bean nào" .-> G
        E -. "" .-> G
    end

    subgraph "4. Container gọi bean sau khi khởi tạo xong"
        G --> H["run(args) được Spring tự gọi
(vì StudentRunner implements CommandLineRunner)"]
    end
```

**Đọc sơ đồ theo thứ tự:**

1. `SpringApplication.run(...)` trong `main` khởi động container, quét package chứa `@SpringBootApplication` để tìm class có stereotype annotation (`@Component`, `@Service`, `@Repository`...).
2. Với mỗi class tìm thấy, Spring tạo 1 bean (instance) — bean nào không phụ thuộc gì (`StudentRepository`) được tạo trước, bean nào phụ thuộc bean khác (`StudentService` cần `StudentRepository`) được tạo sau, theo đúng thứ tự giải quyết phụ thuộc.
3. Khi tạo bean có constructor nhận tham số, Spring tự tìm bean đúng kiểu trong container để truyền vào — đây là **constructor injection**. Nếu tìm thấy **đúng 1** bean phù hợp thì tiêm luôn; nếu tìm thấy **nhiều hơn 1** bean cùng kiểu (như `NotificationService` có cả `EmailNotificationService` và `SmsNotificationService`) mà không có cách phân biệt, Spring báo lỗi `NoUniqueBeanDefinitionException` ngay lúc khởi tạo — phải thêm `@Primary` (chọn 1 bean làm mặc định) hoặc `@Qualifier("tenBean")` (chỉ đích danh) để giải quyết.
4. Sau khi toàn bộ context khởi tạo xong (mọi bean đã có đủ dependency), Spring tự tìm các bean implement `CommandLineRunner` và gọi `run(args)` — không cần code nào gọi tay.

## Tự trả lời: vì sao constructor injection tốt hơn field injection (@Autowired trên field)?

- **Bất biến (immutable) được**: field injection (`@Autowired private StudentRepository repo;`) bắt buộc field không phải `final`, vì Spring tạo object bằng constructor rỗng trước rồi mới gán field sau. Constructor injection cho phép khai báo field `final`, gán đúng 1 lần, không ai sửa lại được.
- **Không thể tồn tại ở trạng thái thiếu phụ thuộc**: với constructor injection, object chỉ được tạo ra khi đã có đủ tham số bắt buộc — không có khoảng thời gian "object đã tồn tại nhưng field đang null" như field injection (object được `new` xong, Spring mới set field ngay sau đó).
- **Test dễ, không cần khởi động Spring**: muốn test `StudentService` chỉ cần `new StudentService(fakeRepository)` bằng tay. Field injection với field `private` thì không có constructor nhận tham số để truyền fake vào, phải nhờ Spring test context hoặc reflection.
- **Phát hiện sớm (fail-fast)**: nếu thiếu bean hoặc dư bean trùng kiểu, constructor injection báo lỗi ngay lúc khởi tạo context (ví dụ lỗi `NoUniqueBeanDefinitionException` khi thử 2 bean cùng implement `NotificationService` ở trên) — sai là biết liền khi chạy `main`, không phải đợi tới lúc gọi field ở runtime mới phát hiện ra null.
- **Lộ rõ class đang phụ thuộc quá nhiều thứ**: constructor có nhiều tham số sẽ "đập vào mắt" ngay khi viết code, là tín hiệu nên tách bớt trách nhiệm của class. Field injection cứ thêm `@Autowired` là xong, không có giới hạn rõ ràng nên dễ che giấu vấn đề thiết kế.
