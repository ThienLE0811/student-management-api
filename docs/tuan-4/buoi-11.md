# Buổi 11 — Thứ Tư 18/11/2026 — Lombok, MapStruct, logging, profile

## 1. Thêm Lombok và MapStruct (nhờ AI chuyển mapping thủ công sang StudentMapper)

- Thêm vào `student-api/pom.xml`: dependency `lombok` (optional, version do `spring-boot-starter-parent` quản lý), dependency `mapstruct` (bản `1.6.3` — **tự khai version**, vì `spring-boot-dependencies:4.1.2` không quản lý sẵn version MapStruct như Lombok).
- `Student`, `StudentRequest`, `StudentResponse` đều đã là **record** từ trước → không gắn Lombok (`@Data`/`@Getter`...) lên các record này vì record tự sinh accessor/equals/hashCode, Lombok không có việc gì để làm thêm. Lombok chỉ mới là dependency sẵn sàng dùng cho sau này (ví dụ class exception, class thường không phải record).
- Tạo `mapper/StudentMapper.java`:
  ```java
  @Mapper(componentModel = "spring")
  public interface StudentMapper {
      @Mapping(target = "fullName", source = "full_name")
      @Mapping(target = "birthDate", source = "birth_date")
      @Mapping(target = "createdAt", source = "created_at")
      @Mapping(target = "age", expression = "java(calculateAge(student.birth_date()))")
      StudentResponse toResponse(Student student);

      default int calculateAge(LocalDate birthDate) {
          return Period.between(birthDate, LocalDate.now()).getYears();
      }
  }
  ```
  Phải khai `@Mapping` tường minh cho `fullName`/`birthDate`/`createdAt` vì tên field ở `Student` (snake_case: `full_name`, `birth_date`, `created_at`) không khớp tên với `StudentResponse` (camelCase) — MapStruct không tự match được, record khác tên là phải chỉ tay.
  `age` không có sẵn ở entity (là giá trị tính toán) nên không thể map trực tiếp — phải dùng `expression = "java(...)"` gọi qua một `default method` trong cùng interface.
- Xoá method `toResponse()` thủ công trong `StudentService`, thay bằng gọi `studentMapper::toResponse` trực tiếp ở `getAll`, `getById`, `create`, `update`.

## 2. Cấu hình annotation processor (Lombok + MapStruct) — lỗi hay gặp

Lỗi kinh điển khi dùng chung Lombok và MapStruct: nếu không khai `annotationProcessorPaths` tường minh trong `maven-compiler-plugin`, Maven sẽ tự động discover processor theo thứ tự ngẫu nhiên trên classpath → MapStruct có thể chạy **trước** Lombok, khi đó MapStruct không thấy được getter/field do Lombok sinh ra (vì lúc đó Lombok chưa sinh), dẫn đến lỗi biên dịch kiểu "Unmapped target property" hoặc mapper sinh ra rỗng.

Cách khắc phục (đã làm trong `pom.xml`): khai tường minh 3 processor theo đúng thứ tự:
```xml
<annotationProcessorPaths>
    <path><!-- lombok --></path>
    <path><!-- lombok-mapstruct-binding --></path>
    <path><!-- mapstruct-processor --></path>
</annotationProcessorPaths>
```
- `lombok` chạy trước để sinh getter/accessor.
- `lombok-mapstruct-binding` là cầu nối bắt buộc khi dùng chung cả hai (theo tài liệu chính thức của MapStruct) — không có nó, thứ tự xử lý vẫn có thể sai dù đã khai processor path.
- `mapstruct-processor` chạy sau cùng, đọc được kết quả Lombok đã sinh.

Trong dự án này chưa có class nào thật sự cần cả Lombok lẫn MapStruct cùng lúc (vì entity/DTO đều là record), nên chưa kiểm chứng được lỗi ordering này bằng thực tế — chỉ cấu hình đúng theo khuyến nghị để phòng khi sau này thêm class thường dùng `@Getter` làm nguồn cho MapStruct.

## 3. Build & kiểm tra code MapStruct sinh ra

- `./mvnw clean compile` chạy sạch, không lỗi annotation processing.
- Mở `target/generated-sources/annotations/com/thienlv/student_api/mapper/StudentMapperImpl.java`: MapStruct sinh đúng 1 class `@Component` implement `StudentMapper`, gán từng field theo đúng `@Mapping` đã khai, gọi `calculateAge(student.birth_date())` cho field `age`. Đối chiếu logic giống hệt method `toResponse()` cũ, không có field nào bị bỏ sót hay gán sai.
- `./mvnw test`: context Spring load được, `StudentMapperImpl` được Spring nhận là bean (`componentModel = "spring"`) — không có lỗi "No qualifying bean" khi `StudentService` inject `StudentMapper`.
