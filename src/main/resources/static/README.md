# static

前端静态资源目录。

## 患者端（中老年友好 · Vue 3）

由 `frontend/` 构建，特点：

- 大字体、大按钮，操作简单
- 只需描述病情，Agent 帮看科、帮挂号
- 智能推荐**默认隐藏**，用户需要时再点展开

```bash
cd frontend && npm run dev    # 开发
cd frontend && npm run build  # 构建到本目录
```

## 管理后台

- `admin.html` / `admin.js`
- `config.js` — 可选 API 地址配置

访问：`http://localhost:8080/`（患者端）、`http://localhost:8080/admin.html`（后台）
