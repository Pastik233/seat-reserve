package com.seat.reserve.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 对应 sys_user 表 */
@Data
public class SysUser {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String studentNo;
    private String roleCode;
    /** 1 正常 0 禁用 */
    private Integer status;
    private Integer violationCount;
    /** 拉黑截止时间，null 表示未拉黑 */
    private LocalDateTime blacklistUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}