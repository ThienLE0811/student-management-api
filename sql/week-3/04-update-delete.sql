
-- case update đầy đủ
BEGIN;
UPDATE students SET email = 'thienlv@gmail.com' WHERE id = 10 RETURNING *;
SELECT * FROM students WHERE id = 10;
ROLLBACK;

-- Case DELETE tương tự, với BEGIN...ROLLBACK + RETURNING *:
BEGIN;
DELETE FROM students WHERE id = 9 RETURNING *;
SELECT * FROM students WHERE id = 9;
ROLLBACK;

-- Case cố tình quên WHERE:
BEGIN;
UPDATE students SET full_name  = 'Lê Văn Thiên';
SELECT * FROM students;
ROLLBACK;