-- Buổi 12 — Thứ Năm 19/11/2026
-- Chuẩn hóa 1NF–3NF ở mức thực dụng; khi nào chấp nhận lặp dữ liệu.
--
-- Đề bài: cho 1 bảng "phẳng" kiểu Excel (sinh viên, lớp, khóa học, điểm đều
-- nằm trên cùng 1 dòng), tách thành các bảng chuẩn (1NF -> 2NF -> 3NF),
-- rồi so sánh với schema hiện có (students/classes/courses/enrollments ở
-- sql/week-3/06-schema.sql).
--
-- Phần 0 (AI sinh dữ liệu mẫu, theo đúng ngoại lệ "DB song song" trong README)
-- là bảng phẳng xuất phát điểm. Từ Phần 1 trở đi là phần tự làm.

-- ============================================================
-- Phần 0: Bảng phẳng kiểu Excel (vùng giao AI — sinh dữ liệu mẫu)
-- ============================================================
-- Một dòng = 1 sinh viên ghi danh 1 khóa học, kèm toàn bộ thông tin lớp/khóa
-- học/điểm nhét chung trên cùng dòng (y như copy từ Excel ra).

DROP TABLE IF EXISTS bang_phang;

CREATE TABLE bang_phang (
    student_id    BIGINT,
    student_name  VARCHAR(100),
    student_email VARCHAR(100),
    class_id      BIGINT,
    class_name    VARCHAR(100),
    teacher_name  VARCHAR(100),
    course_id     BIGINT,
    course_name   VARCHAR(100),
    credit        INT,
    score         NUMERIC
);

INSERT INTO bang_phang
(student_id, student_name, student_email, class_id, class_name, teacher_name, course_id, course_name, credit, score)
VALUES
    (1, 'Nguyễn Văn An',   'an.nguyenvan@gmail.com', 1, 'CNTT-K1', 'Cô Lan',  1, 'Cơ sở tin học',       3, 8.5),
    (1, 'Nguyễn Văn An',   'an.nguyenvan@gmail.com', 1, 'CNTT-K1', 'Cô Lan',  2, 'Kĩ thuật lập trình',  3, 7.0),
    (2, 'Trần Thị Bích',   'bich.tran@gmail.com',    1, 'CNTT-K1', 'Cô Lan',  1, 'Cơ sở tin học',       3, 9.0),
    (3, 'Lê Hoàng Cường',  'cuong.le@gmail.com',     2, 'CNTT-K2', 'Thầy Nam',1, 'Cơ sở tin học',       3, 6.5),
    (3, 'Lê Hoàng Cường',  'cuong.le@gmail.com',     2, 'CNTT-K2', 'Thầy Nam',2, 'Kĩ thuật lập trình',  3, 7.5),
    (4, 'Phạm Minh Đức',   'duc.pham@gmail.com',     2, 'CNTT-K2', 'Thầy Nam',2, 'Kĩ thuật lập trình',  3, 8.0);

-- Nhìn nhanh dữ liệu để thấy rõ chỗ bị lặp (class_name/teacher_name lặp theo
-- class_id, course_name/credit lặp theo course_id):
SELECT * FROM bang_phang ORDER BY student_id, course_id;


-- ============================================================
-- Phần 1: Soi 1NF
-- ============================================================
-- Câu hỏi tự trả lời (ghi luôn vào comment bên dưới):
-- - Mỗi ô trong bang_phang đã là giá trị nguyên tử (atomic) chưa, hay có cột
--   nào đang nhét nhiều giá trị vào 1 ô (vd: 1 sinh viên học nhiều môn mà
--   liệt kê "Cơ sở tin học, Kĩ thuật lập trình" chung 1 ô course_name)?
-- - Với cách thiết kế bang_phang ở Phần 0 (mỗi dòng = 1 lượt ghi danh),
--   bảng này đã đạt 1NF chưa? Vì sao?
--
-- Trả lời:
-- - Mỗi ô đều chỉ chứa 1 giá trị duy nhất (atomic), không có ô nào nhét
--   nhiều giá trị (vd không có ô course_name nào kiểu "Cơ sở tin học, Kĩ
--   thuật lập trình") -> đạt tiêu chí atomic value của 1NF.
-- - Lúc đầu nghĩ có thể lấy student_id làm PRIMARY KEY, nhưng sai: student_id
--   bị lặp lại ở nhiều dòng (1 sinh viên ghi danh nhiều khóa học), nên
--   student_id một mình không phân biệt được từng dòng (insert trùng sẽ bị
--   lỗi duplicate key, giống lỗi UNIQUE đã gặp ở 06-schema.sql).
-- - Khóa chính đúng phải là khóa phức hợp (student_id, course_id) - chỉ tổ
--   hợp 2 cột này mới đảm bảo không có 2 dòng nào trùng nhau.
-- => Kết luận: bang_phang đạt 1NF (atomic value + có khóa phân biệt được
--    từng dòng, nhưng là khóa phức hợp chứ không phải khóa đơn).


-- ============================================================
-- Phần 2: Soi 2NF (chỉ áp dụng khi khóa chính là khóa phức hợp)
-- ============================================================
-- Gợi ý suy nghĩ:
-- - Nếu coi khóa chính của bang_phang là (student_id, course_id), cột nào
--   chỉ phụ thuộc vào student_id (không cần course_id), cột nào chỉ phụ
--   thuộc course_id (không cần student_id)? Đó chính là "phụ thuộc bộ phận"
--   (partial dependency) vi phạm 2NF.
-- - Tách các cột đó ra bảng riêng, chỉ giữ lại ở bang_phang (hoặc bảng n-n)
--   những cột thực sự phụ thuộc vào CẢ HAI cột khóa.
--
-- Trả lời:
-- - Chỉ phụ thuộc student_id (phụ thuộc bộ phận, không cần course_id):
--     student_name, student_email, class_id, class_name, teacher_name
-- - Chỉ phụ thuộc course_id (phụ thuộc bộ phận, không cần student_id):
--     course_name, credit
-- - Phụ thuộc vào CẢ HAI student_id và course_id (đúng chuẩn, không vi phạm
--   2NF, vì đây là dữ liệu thực sự gắn với 1 lượt ghi danh cụ thể):
--     score
-- => Kết luận: bang_phang VI PHẠM 2NF, vì có cột phụ thuộc bộ phận (chỉ cần
--    1 trong 2 cột khóa, không cần cả cặp). Cần tách riêng:
--    - 1 bảng cho các cột chỉ phụ thuộc student_id (khóa: student_id)
--    - 1 bảng cho các cột chỉ phụ thuộc course_id (khóa: course_id)
--    - bảng gốc chỉ giữ lại (student_id, course_id, score) - đúng phụ thuộc
--      cả khóa phức hợp.


-- ============================================================
-- Phần 3: Soi 3NF (phụ thuộc bắc cầu)
-- ============================================================
-- Gợi ý suy nghĩ:
-- - Sau khi tách được bảng "sinh viên" (từ Phần 2), teacher_name có phụ
--   thuộc trực tiếp vào student_id không, hay nó phụ thuộc vào class_id,
--   và class_id mới phụ thuộc vào student_id? (A -> B -> C, không phải
--   A -> C trực tiếp) — đó là phụ thuộc bắc cầu (transitive dependency).
-- - Áp dụng tương tự cho course_name/credit và course_id.
--
-- Trả lời:
-- - Trong bảng "sinh viên" (student_id, student_name, student_email, class_id,
--   class_name, teacher_name): class_name và teacher_name KHÔNG phụ thuộc
--   trực tiếp vào student_id, mà phụ thuộc bắc cầu qua class_id
--   (student_id -> class_id -> class_name/teacher_name). Cần tách tiếp
--   class_name/teacher_name ra 1 bảng "lớp" riêng (khóa: class_id), bảng
--   "sinh viên" chỉ giữ class_id làm khóa ngoại.
-- - Trong bảng "khóa học" (course_id, course_name, credit): course_name và
--   credit phụ thuộc TRỰC TIẾP vào course_id, không qua cột trung gian nào
--   -> không có phụ thuộc bắc cầu, bảng này đã đạt 3NF, không cần tách thêm.
-- => Kết luận: chỉ bảng "sinh viên" (từ Phần 2) vi phạm 3NF, cần tách thêm
--    thành "sinh viên" (student_id, student_name, student_email, class_id)
--    + "lớp" (class_id, class_name, teacher_name).


-- ============================================================
-- Phần 4: Tự viết CREATE TABLE cho các bảng đã chuẩn hóa
-- ============================================================
-- Viết lại từ đầu dựa trên kết quả phân tích ở Phần 1-3, KHÔNG xem trước
-- sql/week-3/06-schema.sql. So sánh ở Phần 5.

-- TODO: CREATE TABLE ...
CREATE TABLE classes
(
    id    BIGINT PRIMARY KEY,
    class_name VARCHAR(100),
    teacher_name VARCHAR(100)
);

CREATE TABLE courses
(
    course_id    BIGINT PRIMARY KEY,
    course_name VARCHAR(100),
    credit      INT
);

CREATE TABLE students (
    id    BIGINT PRIMARY KEY ,
    student_name  VARCHAR(100),
    student_email VARCHAR(100),
    class_id       BIGINT,
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE RESTRICT
);

CREATE TABLE scores
(
    course_id    BIGINT,
    student_id BIGINT,
    score      NUMERIC,

    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE RESTRICT
);


-- ============================================================
-- Phần 5: So sánh với schema hiện có
-- ============================================================
-- Mở sql/week-3/06-schema.sql (students, classes, courses, enrollments),
-- so với bảng bạn vừa tách ở Phần 4:
-- - Giống/khác gì về số bảng, tên cột, khóa chính/khóa ngoại?
-- - Bảng enrollments đã giải quyết phụ thuộc bộ phận/bắc cầu đúng cách
--   tương tự bạn chưa, hay bạn tách khác đi (và vẫn đúng)?

-- TODO: ghi nhận xét so sánh
--
-- Trả lời:
-- - Về Ý TƯỞNG thì giống nhau: cùng tách ra 4 thực thể sinh viên/lớp/khóa
--   học/bảng trung gian ghi danh, cùng dùng FK để nối, tên cột khác nhau
--   chỉ là khác convention đặt tên (full_name vs student_name, name vs
--   class_name...), không phải khác biệt về thiết kế.
-- - Khác biệt THẬT SỰ số 1 - khóa chính của bảng ghi danh:
--     + scores (bản tự làm): PRIMARY KEY (student_id, course_id) - khóa
--       phức hợp trực tiếp trên 2 cột FK.
--     + enrollments (schema thật): có thêm cột "id" riêng làm PRIMARY KEY
--       (surrogate key, GENERATED ALWAYS AS IDENTITY), và dùng
--       UNIQUE(student_id, course_id) để đảm bảo không trùng cặp.
--   Cả 2 cách đều chặn được trùng cặp (student_id, course_id) như nhau, chỉ
--   khác ở chỗ enrollments có thêm 1 "tên định danh" gọn (1 số id) cho từng
--   dòng ghi danh, tiện khi cần bảng khác tham chiếu tới ĐÚNG 1 dòng ghi
--   danh cụ thể (chỉ cần 1 cột FK thay vì 2), hoặc dùng làm id trong URL
--   REST API (/api/enrollments/{id}).
-- - Khác biệt THẬT SỰ số 2 - ON DELETE CASCADE (bản tự làm) vs ON DELETE
--   RESTRICT (schema thật) cho students.class_id -> classes.id:
--     + CASCADE: xóa 1 lớp sẽ tự động xóa luôn mọi sinh viên thuộc lớp đó
--       - nguy hiểm nếu xóa nhầm lớp, mất dữ liệu không chủ ý.
--     + RESTRICT: xóa lớp còn sinh viên sẽ bị CHẶN (báo lỗi), bắt buộc
--       phải tự xử lý sinh viên trước (chuyển lớp/xóa) rồi mới xóa được
--       lớp - an toàn hơn vì sinh viên vẫn có ý nghĩa tồn tại độc lập với
--       lớp, không nên bị xóa tự động chỉ vì lớp của họ bị xóa.
-- => Kết luận: cấu trúc bảng (chuẩn hóa) giống nhau, nhưng 2 quyết định kỹ
--    thuật (surrogate key cho bảng n-n, và ON DELETE RESTRICT thay vì
--    CASCADE cho quan hệ lớp-sinh viên) là khác biệt thật sự, không phải
--    chỉ là style đặt tên.


-- ============================================================
-- Phần 6: Khi nào CHẤP NHẬN lặp dữ liệu (denormalize có chủ đích)?
-- ============================================================
-- Gợi ý câu hỏi để tự trả lời, không phải lý thuyết suông:
-- - Nếu trang "Danh sách sinh viên" của API luôn cần hiển thị kèm tên lớp
--   (class_name) cho mỗi sinh viên, và bảng students có hàng triệu dòng,
--   JOIN sang classes mỗi lần load danh sách có đáng lo về performance
--   không? Khi nào thì bạn chấp nhận lưu thêm 1 cột class_name ngay trong
--   students (lặp dữ liệu so với 3NF) để đổi lấy tốc độ đọc?
-- - Đánh đổi gì khi chọn lặp dữ liệu: cái gì được lợi, cái gì phải tự lo
--   (gợi ý: đồng bộ dữ liệu khi tên lớp đổi tên).

-- TODO: tự trả lời
