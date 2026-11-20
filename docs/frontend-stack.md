# 前端技术栈说明

> 记录时间：2026-11-12 ｜ 最后更新：2026-11-20（React → Vue 3 迁移）

## 1. 选型结论

前端用 **Vue 3**。

前端在这个项目里只承担「把接口跑通、能演示」的职责，重心是后端的并发控制与状态机。2026-11-20 从 React 19 全量迁移到 Vue 3，语法更简洁，且 Vue Router 的动态 import 带来了路由懒加载（各页面拆成独立 chunk）。

| 层 | 选型 | 用途 |
| --- | --- | --- |
| 框架 | Vue 3.5 | Composition API + `<script setup>`，单文件组件（SFC） |
| 构建 | Vite 8 | 开发服务器与打包 |
| 语言 | JavaScript | 不上 TypeScript |
| UI 库 | Element Plus 2.11 | el-table / el-form / el-dialog / ElMessage |
| 状态 | Pinia 3 | 只存 token 与用户信息，持久化到 localStorage |
| 路由 | Vue Router 4 | 6 条路由 + 登录守卫 + ADMIN 角色守卫，路由懒加载 |
| HTTP | Axios | 拦截器统一处理 401 与错误提示 |
| 时间 | dayjs | 时段计算与格式化 |

## 2. 目录与构建约定

```
web/
├── index.html
├── vite.config.js        # /api 代理到 127.0.0.1:8080；build.outDir 指向 ../src/main/resources/static
├── eslint.config.js      # @eslint/js + eslint-plugin-vue（flat config）
└── src/
    ├── main.js           # 入口：createApp + Pinia + Router + ElementPlus（中文 locale）
    ├── App.vue           # 布局、菜单、退出、/auth/me 校验
    ├── api/request.js    # axios 实例与拦截器（ElMessage 提示）
    ├── router/index.js   # 路由表 + beforeEach 守卫
    ├── stores/user.js    # Pinia store，token 持久化
    └── views/            # 6 个页面组件
        ├── Login.vue
        ├── SeatList.vue
        ├── MyBookings.vue
        ├── AdminOverview.vue
        ├── AdminSeats.vue
        └── AdminBookings.vue
```

- **开发**：`npm run dev`，Vite 把 `/api` 代理到 8080，前后端各自热更新。
- **构建**：`npm run build` 直接输出到 `src/main/resources/static`，由 Spring Boot 作为静态资源提供，最终打成一个 jar。
- **刷新路由**：`/seats`、`/admin/seats` 这些前端路由由 `SpaForwardController` forward 回 `index.html`，不会 404；安全白名单里也放行了这几条路径。

> 改了 `web/` 下的源码必须重新 `npm run build`——前端是构建进静态资源随 jar 发布的，运行中的实例读的是构建时的版本。

## 3. 几处实现约定

- **登录态**：Pinia 持久化 token，路由守卫判断 token 是否存在 + 管理员角色；有效性由 `/api/auth/me` 验证，401 交给拦截器统一跳登录。
- **管理员守卫**：`meta.requiresAdmin` 的路由在 `beforeEach` 里校验 `user.roleCode === 'ADMIN'`，非管理员重定向回 `/seats`。前端守卫只是体验，真正的边界是后端 `@PreAuthorize("hasRole('ADMIN')")`。
- **错误提示**：集中在 axios 拦截器用 `ElMessage`，页面里的 `catch` 不再重复弹消息，避免一次失败弹两次。
- **时段选项**：按阅览室的 `open_time / close_time` 生成，不写死全局时段；默认起点取当前时间向上取整，避免默认值就是「已经错过的时段」。Vue 里这些派生值用 `computed` 实现，天然没有 React 那种 setState 时序问题。
- **时间格式**：日期用 `YYYY-MM-DD`、时间用 `HH:mm:ss`，与后端 `@DateTimeFormat` 的 ISO 约定对齐。
- **路由懒加载**：每个页面用 `() => import('../views/Xxx.vue')` 动态导入，Vite 会拆成独立 chunk，首屏只加载入口与当前页。
