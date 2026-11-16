-- Buổi 22 — Flyway V4: thêm capacity + enrolled_count cho classes, phục vụ
-- bài optimistic locking (POST /api/classes/{id}/students, 409 khi hết chỗ).

ALTER TABLE classes
    ADD COLUMN capacity       INT NOT NULL DEFAULT 30,
    ADD COLUMN enrolled_count INT NOT NULL DEFAULT 0;

-- Đồng bộ enrolled_count theo đúng số sinh viên hiện có trong mỗi lớp (dữ liệu
-- dev cũ đã có sẵn sinh viên trước khi cột này tồn tại).
UPDATE classes c
SET enrolled_count = sub.cnt
FROM (SELECT class_id, COUNT(*) AS cnt FROM students GROUP BY class_id) sub
WHERE c.id = sub.class_id;

-- Đảm bảo lớp dev cũ không bị "quá tải" ngay sau migrate (capacity luôn >=
-- enrolled_count hiện tại).
UPDATE classes SET capacity = enrolled_count WHERE enrolled_count > capacity;

ALTER TABLE classes
    ALTER COLUMN capacity DROP DEFAULT,
    ALTER COLUMN enrolled_count DROP DEFAULT;
