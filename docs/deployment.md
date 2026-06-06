# 部署说明

## 环境要求

- JDK 17
- Docker Desktop 或可用的 MySQL、MongoDB、Redis、Kafka 实例
- Maven Wrapper 所需网络，首次构建会下载 Maven 和依赖
- 可选: OpenAI 兼容 Chat API、Embedding API、Pinecone Index

## 本地演示部署

1. 复制配置:

```powershell
Copy-Item .env.example .env
```

2. 启动依赖:

```powershell
.\scripts\start-deps.ps1 -WithKafka
```

3. 启动服务:

```powershell
.\scripts\start-app.ps1 -JavaHome "C:\Program Files\Java\jdk-17" -Profile dev -Port 8080
```

4. 验证:

```powershell
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```

如果当前机器没有 Docker 或云端密钥，可以使用离线演示 profile:

```powershell
.\scripts\run-demo.ps1 -JavaHome "C:\Program Files\Java\jdk-17" -Port 8080
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```

离线演示使用 H2、内存向量库、本地库存和本地事件，不验证 MySQL、MongoDB、Redis、Kafka、OpenAI、Pinecone 的真实连通性。

## 正式环境部署

1. 创建数据库并执行 `mysql-schema-v1.sql`。
2. 创建 MongoDB 数据库，初始化脚本参考 `docker/mongodb/init/01-indexes.js`。
3. 准备 Redis 实例，确保应用可以读写号源库存 Key。
4. 准备 Kafka topic，默认 topic 为 `registration.reserved`。
5. 准备 `.env`，至少设置:

```properties
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-host:3306/doctor?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
SPRING_DATASOURCE_USERNAME=doctor
SPRING_DATASOURCE_PASSWORD=change-me
MONGODB_URI=mongodb://mongo-host:27017/doctor
REDIS_HOST=redis-host
REDIS_PORT=6379
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092
APP_AI_PROVIDER=openai
APP_VECTOR_STORE_PROVIDER=pinecone
APP_STOCK_PROVIDER=redis
APP_EVENT_PROVIDER=kafka
OPENAI_API_KEY=change-me
OPENAI_EMBEDDING_API_KEY=change-me
PINECONE_API_KEY=change-me
PINECONE_INDEX_HOST=https://your-index-host
```

6. 构建 Jar:

```powershell
.\mvnw.cmd clean package
```

7. 运行:

```powershell
java -jar target\intelligent-doctor-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 健康检查

- `GET /actuator/health`
- `GET /api/system/profile`

`/api/system/profile` 应显示 MySQL、MongoDB、Redis、Kafka 为 `up`，OpenAI 和 Pinecone 在配置密钥后应显示 `configured`。

## 回滚

应用没有破坏性启动迁移。回滚时停止当前进程，恢复上一版 Jar 和上一版 `.env`，再重新启动。数据库结构变更需按对应 SQL 变更脚本人工回滚。
