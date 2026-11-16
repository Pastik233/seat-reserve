# 前端技术栈说明

> 记录时间：2026-11-12 ｜ 最后更新：2026-11-16

## 1. 选型结论

前端用 **React 19**。

前端在这个项目里只承担「把接口跑通、能演示」的职责，重心是后端的并发控制与状态机；选自己更熟的栈，可以把时间留给后端。

| 层 | 选型 | 用途 |
| --- | --- | --- |
| 框架 | React 19 | 函数组件 + Hooks |
| 构建 | Vite 8 | 开发服务器与打包 |
| 语言 | JavaScript | 不上 TypeScript |
| UI 库 | Ant Design 6 | Table / DatePicker / message / Layout |
| 状态 | Zustand 5 | 只存 token 与用户信息，持久化到 localStorage |
| 路由 | React Router 7 | 三条路由 + 登录守卫 |
| HTTP | Axios | 拦截器统一处理 401 与错误提示 |
| 时间 | dayjs | 时段计算与格式化 |

## 2. 目录与构建约定

```
web/
├── index.html
├── vite.config.js      # /api 代理到 127.0.0.1:8080；build.outDir 指向 ../src/main/resources/static
└── src/
    ├── api/request.js  # axios 实例与拦截器
    ├── pages/          # Login / SeatList / MyBookings
    ├── store/user.js   # Zustand，token 持久化
    ├── App.jsx         # 布局、路由表、登录守卫
    └── main.jsx        # 入口：ConfigProvider + BrowserRouter
```

- **开发**：`npm run dev`，Vite 把 `/api` 代理到 8080，前后端各自热更新。
- **构建**：`npm run build` 直接输出到 `src/main/resources/static`，由 Spring Boot 作为静态资源提供，最终打成一个 jar。
- **刷新路由**：`/seats`、`/my` 这些前端路由由 `SpaForwardController` forward 回 `index.html`，不会 404；安全白名单里也放行了这几条路径。

## 3. 几处实现约定

- **登录态**：Zustand 持久化 token，路由守卫只判断 token 是否存在；有效性由 `/api/auth/me` 验证，401 交给拦截器统一跳登录。
- **错误提示**：集中在 axios 拦截器，页面里的 `catch` 不再重复弹消息，避免一次失败弹两次。
- **时段选项**：按阅览室的 `open_time / close_time` 生成，不写死全局时段；默认起点取当前时间向上取整，避免默认值就是「已经错过的时段」。
- **时间格式**：日期用 `YYYY-MM-DD`、时间用 `HH:mm:ss`，与后端 `@DateTimeFormat` 的 ISO 约定对齐。
