# docker

本目录保存本地开发依赖的 Docker 配置。

- `mysql/init`: MySQL 建表初始化脚本。
- `mongodb/init`: MongoDB 集合索引初始化脚本。
- 根目录 `docker-compose.yml` 会启动 MySQL、MongoDB、Redis，并可通过 profile 启动 Kafka。

本地演示时可执行:

```powershell
.\scripts\start-deps.ps1 -WithKafka
```
