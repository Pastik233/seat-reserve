package com.seat.reserve;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 全量上下文启动测试。
 *
 * <p>启动过程会连 MySQL 执行 Flyway、连 Redis 初始化连接池，属于集成测试范畴，
 * 在没有中间件的环境（CI、刚 clone 的机器）里必然失败，所以默认禁用。
 * 本地把 {@code docker compose up -d} 起好之后，去掉 {@code @Disabled} 即可验证上下文完整性。
 * 纯逻辑断言请写在不需要 Spring 上下文的单元测试里（见 BookingValidatorTest）。
 */
@Disabled("依赖真实 MySQL + Redis，本地起好依赖后手动开启")
@SpringBootTest
class SeatReserveApplicationTests {

    @Test
    void contextLoads() {
    }
}
