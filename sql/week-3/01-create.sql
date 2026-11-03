-- Buổi 1 — tạo bảng students và seed 10 dòng dữ liệu mẫu

CREATE TABLE students (
  id BIGINT PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100),
  birth_date DATE,
  created_at TIMESTAMP
);

INSERT INTO students (id, full_name, email, birth_date, created_at) VALUES
(1,  'Nguyễn Văn An',      'an.nguyenvan@gmail.com',     '2004-03-15', '2026-09-01 08:15:23'),
(2,  'Trần Thị Bích Ngọc', 'ngoc.tranthibich@gmail.com', '2005-07-22', '2026-09-01 09:42:10'),
(3,  'Lê Hoàng Minh',      'minh.lehoang@gmail.com',     '2003-11-08', '2026-09-02 14:05:47'),
(4,  'Phạm Thu Hà',        'ha.phamthu@gmail.com',       '2004-01-30', '2026-09-03 10:30:00'),
(5,  'Hoàng Đức Thắng',    'thang.hoangduc@gmail.com',   '2002-09-19', '2026-09-05 16:18:35'),
(6,  'Vũ Thị Mai Anh',     'anh.vuthimai@gmail.com',     '2005-12-04', '2026-09-08 11:02:59'),
(7,  'Đặng Quốc Huy',      'huy.dangquoc@gmail.com',     '2004-06-27', '2026-09-10 13:47:12'),
(8,  'Bùi Phương Linh',    'linh.buiphuong@gmail.com',   '2003-04-11', '2026-09-15 09:20:44'),
(9,  'Đỗ Tuấn Kiệt',       'kiet.dotuan@gmail.com',      '2005-02-18', '2026-10-01 15:55:08'),
(10, 'Ngô Thị Thanh Hương','huong.ngothithanh@gmail.com','2004-10-25', '2026-10-12 08:40:31');
