-- V3：运营报表 + 通知发件箱

-- (1) 通知收件人：用户表补邮箱（空串表示只用日志通道）
ALTER TABLE sys_user
    ADD COLUMN email VARCHAR(128) NOT NULL DEFAULT '' COMMENT '通知邮箱，空则走日志通道'
    AFTER student_no;
UPDATE sys_user SET email = 'admin@study.local' WHERE username = 'admin';
UPDATE sys_user SET email = 'zhangsan@study.local' WHERE username = 'zhangsan';

-- (2) 报表索引：按日期区间 + 状态聚合
ALTER TABLE seat_booking ADD KEY idx_date_status (book_date, status);
ALTER TABLE booking_log   ADD KEY idx_created_at (created_at);

-- (3) 通知发件箱：和业务数据同事务落库，worker 异步投递
CREATE TABLE notification_task
(
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id    BIGINT      NOT NULL,
    user_id       BIGINT      NOT NULL,
    event_type    VARCHAR(32) NOT NULL COMMENT 'BOOKED/CANCELLED/CHECKED_IN/NO_SHOW/OVERTIME/FINISHED',
    channel       VARCHAR(16) NOT NULL DEFAULT 'LOG' COMMENT 'LOG/EMAIL',
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SENT/FAILED',
    retry_count   INT         NOT NULL DEFAULT 0,
    next_retry_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at       DATETIME NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 唯一键 = 幂等保证：同一预约同一事件只落一条，重复调用被数据库挡住
    UNIQUE KEY uk_booking_event (booking_id, event_type),
    KEY        idx_pending (status, next_retry_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '通知发件箱';
