package com.seat.reserve.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 管理端新增 / 修改座位的入参。
 * 注意：不含 roomId 之外的不可变字段约束说明——新增时 roomId 必填，
 * 修改时忽略 roomId（座位归属不允许变更，避免打乱已有预约）。
 */
public record SeatUpsertRequest(
        @NotNull(message = "阅览室不能为空") Long roomId,
        @NotBlank(message = "座位号不能为空")
        @Size(max = 16, message = "座位号最多 16 个字符")
        @Pattern(regexp = "[A-Za-z0-9\\-]+", message = "座位号只能包含字母、数字和连字符")
        String seatNo,
        @Size(max = 32, message = "分区最多 32 个字符") String zone,
        @NotNull(message = "是否有插座不能为空")
        @Min(value = 0, message = "hasPower 只能是 0 或 1")
        @Max(value = 1, message = "hasPower 只能是 0 或 1") Integer hasPower,
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "status 只能是 0 或 1")
        @Max(value = 1, message = "status 只能是 0 或 1") Integer status
) {
}
