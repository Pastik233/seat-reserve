package com.seat.reserve.common;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 把「Redis 副作用」推迟到事务提交之后再执行。
 *
 * <p>Redis 不参与数据库事务。在事务内就增删延迟任务、写缓存，一旦事务回滚，
 * 数据库还是旧状态而 Redis 已经被改过，两边就永久不一致了 ——
 * 比如取消预约时先摘掉延迟任务再回滚，这条预约就永远不会自动过期，座位被彻底占死。
 *
 * <p>当前没有事务时直接执行，方便在非事务上下文（如定时任务）里复用。
 */
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
