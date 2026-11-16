-- 种子数据：账号、阅览室、座位、一条演示预约

INSERT INTO sys_user (id, username, password, nickname, student_no, role_code)
VALUES (1, 'admin', '{noop}123456', '管理员', 'T0001', 'ADMIN'),
       (2, 'zhangsan', '{noop}123456', '张三', '2023001', 'USER');

INSERT INTO lib_room (id, name, floor, open_time, close_time)
VALUES (1, '一楼自习室', '1F', '08:00:00', '22:00:00'),
       (2, '三楼阅览室', '3F', '08:30:00', '21:30:00');

-- 房间 1 的 12 个座位（A/B 两区，A 区带插座）
INSERT INTO lib_seat (room_id, seat_no, zone, has_power)
VALUES (1, 'A-01', 'A区', 1),
       (1, 'A-02', 'A区', 1),
       (1, 'A-03', 'A区', 1),
       (1, 'A-04', 'A区', 1),
       (1, 'A-05', 'A区', 1),
       (1, 'A-06', 'A区', 1),
       (1, 'B-01', 'B区', 0),
       (1, 'B-02', 'B区', 0),
       (1, 'B-03', 'B区', 0),
       (1, 'B-04', 'B区', 0),
       (1, 'B-05', 'B区', 0),
       (1, 'B-06', 'B区', 0),
       (2, 'C-01', 'C区', 1),
       (2, 'C-02', 'C区', 1),
       (2, 'C-03', 'C区', 1),
       (2, 'C-04', 'C区', 0),
       (2, 'C-05', 'C区', 0),
       (2, 'C-06', 'C区', 0);

-- 用 CURDATE() 保证任何一天都能演示
INSERT INTO seat_booking (seat_id, room_id, user_id, book_date, start_time, end_time, status)
VALUES (1, 1, 2, CURDATE(), '18:00:00', '20:00:00', 'BOOKED');

INSERT INTO booking_log (booking_id, from_status, to_status, operator_id)
VALUES (1, '', 'BOOKED', 2);