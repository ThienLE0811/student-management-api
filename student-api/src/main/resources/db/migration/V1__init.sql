-- Buổi 19 — Flyway V1: schema khởi tạo, đối chiếu đúng entity Java hiện tại
-- (nguồn sự thật cho schema là entity, không phải sql/week-3/06-schema.sql —
-- file đó vẫn là track học riêng "DB song song", không tự đồng bộ).
-- Thứ tự bảng theo phụ thuộc khóa ngoại: classes, courses (không phụ thuộc)
-- -> students (FK tới classes) -> enrollments (FK tới students + courses).

CREATE TABLE classes (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_classes_name UNIQUE (name)
);

CREATE TABLE courses (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_courses_name UNIQUE (name)
);

CREATE TABLE students (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name  VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL,
    class_id   BIGINT NOT NULL,
    CONSTRAINT uk_students_email UNIQUE (email),
    CONSTRAINT fk_students_class_id FOREIGN KEY (class_id) REFERENCES classes (id)
);

CREATE TABLE enrollments (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id  BIGINT NOT NULL,
    score      DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_enrollments_student_id FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_enrollments_course_id FOREIGN KEY (course_id) REFERENCES courses (id),
    CONSTRAINT uk_enrollment_student_course UNIQUE (student_id, course_id)
);
