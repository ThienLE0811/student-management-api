-- Buổi 20 — Flyway V3: bảng lịch sử chuyển lớp, ghi lại mỗi lần
-- PUT /api/students/{id}/class/{classId} đổi lớp cho 1 sinh viên.

CREATE TABLE class_transfer_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id      BIGINT NOT NULL,
    from_class_id   BIGINT NOT NULL,
    to_class_id     BIGINT NOT NULL,
    transferred_at  TIMESTAMP NOT NULL,
    CONSTRAINT fk_class_transfer_history_student_id FOREIGN KEY (student_id) REFERENCES students (id),
    CONSTRAINT fk_class_transfer_history_from_class_id FOREIGN KEY (from_class_id) REFERENCES classes (id),
    CONSTRAINT fk_class_transfer_history_to_class_id FOREIGN KEY (to_class_id) REFERENCES classes (id)
);
