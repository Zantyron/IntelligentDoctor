# scripts

项目脚本目录，主要用于本地启动、测试和验收。

- `start-deps.ps1`: 启动 Docker 依赖。
- `start-app.ps1`: 使用 Maven 启动 Spring Boot 应用。
- `run-demo.ps1`: 启动离线演示模式，不依赖真实云端模型和向量库。
- `smoke-test.ps1`: 覆盖导诊、挂号和后台导入的冒烟测试。
- `redis-hot-slot-loadtest.ps1`: Redis Lua 热点号源压测，验证库存扣减不超卖。
- `verify-release.ps1`: 发布前综合检查脚本。
