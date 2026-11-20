package com.seat.reserve.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 单页应用的刷新兜底：
 * 前端路由由 React Router 接管，后端只负责把这几条路径都吐 index.html，
 * 剩下的 URL 解析交给浏览器里的 JS。
 * 注意：这里绝不能写成 /** 通配，否则会把 /api/** 也吞掉。
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/seats", "/my", "/admin", "/admin/seats","/admin/bookings"})
    public String index() {
        return "forward:/index.html";
    }
}
