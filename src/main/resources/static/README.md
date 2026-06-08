# static

前端静态资源目录，Spring Boot 启动后会直接暴露这些页面。

- `index.html`: 患者端入口。
- `app.js`: 患者端交互逻辑，包含流式聊天、会话管理和挂号确认。
- `admin.html`: 管理后台入口。
- `admin.js`: 后台登录、文件导入、任务状态和系统状态交互。
- `styles.css`: 医院风格主题、聊天布局和后台页面样式。
- `config.js`: 前端运行时配置。

启动项目后访问:

- 患者端: `http://localhost:8080/`
- 管理后台: `http://localhost:8080/admin.html`
