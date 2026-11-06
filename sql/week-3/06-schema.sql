-- Buổi 6: schema.sql chạy lại được từ đầu (DROP TABLE IF EXISTS rồi CREATE)
-- 4 bảng: students, classes, courses, enrollments (n-n qua enrollments)

CREATE TABLE courses (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    name  VARCHAR(100) NOT NULL,
    credit INT
);

CREATE TABLE enrollments (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    student_id  BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    score  NUMERIC,
    CONSTRAINT fk_enrollments_student_id FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_enrollments_course_id FOREIGN KEY (course_id) REFERENCES courses(id),
    CONSTRAINT uq_enrollments_student_course UNIQUE (student_id, course_id)
);

INSERT INTO courses (name, credit) VALUES ('Cơ sở tin học', 3), ('Kĩ thuật lập trình', 3);

INSERT INTO enrollments (student_id, course_id, score) VALUES
    (3, 1, NULL),
    (3, 2, NULL);

-- Da test them: INSERT INTO enrollments (student_id, course_id, score) VALUES (3, 1, 8.5);
-- -> ERROR 23505 duplicate key value violates unique constraint "uq_enrollments_student_course"
--    Key (student_id, course_id)=(3, 1) already exists.
-- => UNIQUE(student_id, course_id) hoat dong dung, chan duoc 1 sinh vien ghi danh trung 1 mon.

-- ===== Ghi chú / nhật ký =====
-- Quan he n-n giua students va courses: 1 sinh vien hoc nhieu mon, 1 mon co nhieu sinh vien.
-- Khong the luu truc tiep FK trong students hay courses vi 1-n khong du, nen can 1 bang
-- trung gian (junction table) la enrollments, moi dong la 1 luot ghi danh, chua 2 FK
-- (student_id -> students.id, course_id -> courses.id) + du lieu rieng cua luot ghi danh (score).
-- Ve ban chat: students va courses deu la quan he 1-n voi enrollments; quan he n-n giua
-- students/courses chi la "goc nhin" khi noi 2 bang 1-n do qua enrollments.
--
-- UNIQUE(student_id, course_id) dam bao khong co 2 dong enrollments trung cap
-- (student_id, course_id) -> 1 sinh vien khong the ghi danh 2 lan vao cung 1 mon.
--
-- Loi da gap khi viet CREATE TABLE courses: du 1 dau phay sau cot cuoi cung (credit INT,)
-- truoc dau ngoac dong ) -> loi cu phap. Bai hoc: cot cuoi cung trong danh sach khong duoc
-- co dau phay.
--
-- Loi da gap lan dau viet enrollments: quen them CONSTRAINT UNIQUE (student_id, course_id)
-- dung theo yeu cau de bai, chi co 2 FK. Bai hoc: doc lai dung yeu cau de bai truoc khi
-- coi la xong 1 bang, khong chi dua vao pattern da quen (FK) ma quen constraint khac.
--
-- Pham vi file nay chi tao moi courses + enrollments (classes, students da co tu buoi 5),
-- nen file 06-schema.sql nay KHONG tu chay duoc tren 1 DB trong tu dau (enrollments phu
-- thuoc FK toi students da ton tai san). Day la khac biet so voi yeu cau goc cua de bai
-- (sql/schema.sql day du 4 bang chay lai tu dau khong loi) - ghi lai de nho lam bu sau.

