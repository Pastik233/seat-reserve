-- 图书馆座位预约系统 · 初始化建表（6 张表）
-- 字符集统一 utf8mb4，存储引擎 InnoDB

CREATE TABLE sys_user
(
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(64)  NOT NULL,
    password        VARCHAR(128) NOT NULL COMMENT '带编码前缀，如 {noop}123456',
    nickname        VARCHAR(64)  NOT NULL DEFAULT '',
    student_no      VARCHAR(32)  NOT NULL DEFAULT '',
    role_code       VARCHAR(32)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN / USER',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
    violation_count INT          NOT NULL DEFAULT 0 COMMENT '累计违约次数',
    blacklist_until DATETIME NULL COMMENT '拉黑截止时间，NULL 表示未拉黑',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户';

CREATE TABLE lib_room
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    floor      VARCHAR(32)  NOT NULL DEFAULT '',
    open_time  TIME         NOT NULL DEFAULT '08:00:00',
    close_time TIME         NOT NULL DEFAULT '22:00:00',
    status     TINYINT      NOT NULL DEFAULT 1 COMMENT '1开放 0关闭',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '阅览室';

CREATE TABLE lib_seat
(
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_id   BIGINT      NOT NULL,
    seat_no   VARCHAR(16) NOT NULL COMMENT '如 A-01',
    zone      VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'A区/B区/靠窗',
    has_power TINYINT     NOT NULL DEFAULT 0 COMMENT '有无插座',
    status    TINYINT     NOT NULL DEFAULT 1 COMMENT '1可用 0维修',
    UNIQUE KEY uk_room_seat (room_id, seat_no),
    KEY       idx_room (room_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '座位';

CREATE TABLE seat_booking
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    seat_id    BIGINT       NOT NULL,
    room_id    BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    book_date  DATE         NOT NULL,
    start_time TIME         NOT NULL,
    end_time   TIME         NOT NULL,
    status     VARCHAR(16)  NOT NULL DEFAULT 'BOOKED' COMMENT 'BOOKED/CHECKED_IN/LEAVING/FINISHED/CANCELLED/EXPIRED',
    checkin_at DATETIME NULL,
    leave_at   DATETIME NULL COMMENT '最近一次暂离时间',
    version    INT          NOT NULL DEFAULT 1 COMMENT '乐观锁',
    remark     VARCHAR(255) NOT NULL DEFAULT '',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY        idx_seat_date (seat_id, book_date, status, start_time),
    KEY        idx_user_date (user_id, book_date),
    KEY        idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '座位预约';

CREATE TABLE booking_log
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id  BIGINT      NOT NULL,
    from_status VARCHAR(16) NOT NULL DEFAULT '',
    to_status   VARCHAR(16) NOT NULL DEFAULT '',
    operator_id BIGINT      NOT NULL DEFAULT 0 COMMENT '0=系统自动',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY         idx_booking (booking_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '状态流转日志';

CREATE TABLE violation
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    booking_id BIGINT      NOT NULL,
    type       VARCHAR(16) NOT NULL COMMENT 'NO_SHOW 未签到 / OVERTIME 暂离超时',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY        idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '违约记录';