# Intelligent Doctor

Intelligent Doctor 是一个智能导诊与挂号演示系统。项目采用 Spring Boot 单体后端，内置患者端和管理后台静态页面，覆盖导诊对话、RAG 知识召回、挂号草稿、Redis Lua 号源预占、异步订单落库、后台资料导入和外部依赖状态检查。

## 功能范围

- 患者端: `/`，支持症状导诊、智能挂号、RAG 证据、SSE 流式输出、会话管理和挂号确认。
- 管理后台: `/admin.html`，通过 `admin/admin` 登录，支持医院资料导入、向量重建、订单查看和系统状态查看。
- 后端服务: Spring Boot 3.5、Java 17、JPA、MongoDB、Redis、Kafka、OpenAI/Pinecone 可配置接入。
- 测试交付: 单元测试、集成测试、RAG 检索测试、挂号一致性测试、Redis 热点号源压测脚本。

## 项目结构

| 目录 | 说明 |
| --- | --- |
| `src/main/java/com/intelligentdoctor/admin` | 管理后台接口、导入任务、文件解析、知识切片入库和后台鉴权。 |
| `src/main/java/com/intelligentdoctor/ai` | 大模型网关、Prompt 组装、AIService 边界、Function Calling 工具建议和规则兜底。 |
| `src/main/java/com/intelligentdoctor/chat` | 患者端聊天入口、SSE 流式输出、聊天记忆、会话列表和消息删除。 |
| `src/main/java/com/intelligentdoctor/knowledge` | RAG 知识库模块，负责文档切割、Embedding、向量入库、粗排检索和 rerank 精排。 |
| `src/main/java/com/intelligentdoctor/registration` | 智能挂号模块，包含挂号草稿、实名信息、号源预占、订单确认和事件落库。 |
| `src/main/java/com/intelligentdoctor/catalog` | 医院、科室、诊室、医生、排班和挂号规则等主数据查询。 |
| `src/main/java/com/intelligentdoctor/system` | 系统运行状态、外部依赖探活和启动地址输出。 |
| `src/main/resources/static` | 患者端和管理后台静态页面，包含医院风格 UI、等待态、流式输出和后台登录。 |
| `docker` | MySQL、MongoDB、Redis、Kafka 的本地开发容器配置和初始化脚本。 |
| `sample-data` | 演示医院、科室、医生、排班、规则和病症知识库样例。 |
| `scripts` | 一键启动、冒烟测试、发布验证和 Redis 热点号源压测脚本。 |
| `docs` | 架构、接口、部署、演示和验收文档，方便面试或答辩时快速说明项目。 |

核心 Agent 范式为 `react-rag-tool`: 先做意图和 Query 处理，再执行 RAG 检索和工具调用，最后将系统 Prompt、业务 Prompt、历史摘要、RAG 证据和工具结果拼接后发送给大模型。

## 外部依赖

本地 Docker Compose 可启动:

- MySQL `localhost:3306`
- MongoDB `localhost:27017`
- Redis `localhost:6379`
- Kafka `localhost:9092`，默认通过 compose profile 启动

云端或正式环境自行准备:

- OpenAI 兼容 Chat API Key
- OpenAI 兼容 Embedding API Key
- Pinecone API Key 和 Index Host
- 真实医院样例数据，可参考 `sample-data/hospital-import.csv`

## 快速启动

1. 准备 Java 17 和 Docker。

2. 复制配置模板:

```powershell
Copy-Item .env.example .env
```

3. 启动依赖:

```powershell
.\scripts\start-deps.ps1 -WithKafka
```

4. 启动应用:

```powershell
.\scripts\start-app.ps1 -JavaHome "C:\Program Files\Java\jdk-17"
```

没有 Docker 或云端 Key 时，也可以启动离线演示模式:

```powershell
.\scripts\run-demo.ps1 -JavaHome "C:\Program Files\Java\jdk-17"
```

5. 打开页面:

- 患者端: [http://localhost:8080/](http://localhost:8080/)
- 管理后台: [http://localhost:8080/admin.html](http://localhost:8080/admin.html)，账号 `admin`，密码 `admin`
- 依赖状态: [http://localhost:8080/api/system/profile](http://localhost:8080/api/system/profile)

## 验收命令

```powershell
.\mvnw.cmd test
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
.\scripts\redis-hot-slot-loadtest.ps1 -Stock 50 -Concurrency 200
```

压测输出中的 `noOversell` 应为 `true`，表示 Lua 原子扣减未超卖。

## 配置说明

常用配置在 `.env.example` 中。正式部署建议至少覆盖:

```properties
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-host:3306/doctor?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
SPRING_DATASOURCE_USERNAME=doctor
SPRING_DATASOURCE_PASSWORD=change-me
MONGODB_URI=mongodb://mongo-host:27017/doctor
REDIS_HOST=redis-host
KAFKA_BOOTSTRAP_SERVERS=kafka-host:9092
APP_AI_PROVIDER=openai
APP_VECTOR_STORE_PROVIDER=pinecone
APP_STOCK_PROVIDER=redis
APP_EVENT_PROVIDER=kafka
APP_AGENT_PARADIGM=react-rag-tool
OPENAI_API_KEY=change-me
OPENAI_EMBEDDING_API_KEY=change-me
PINECONE_API_KEY=change-me
PINECONE_INDEX_HOST=https://your-index-host
```

测试 profile 使用 H2、内存向量库、本地库存和本地事件，不依赖外部服务。

## 文档

- [架构说明](docs/architecture.md)
- [接口文档](docs/api.md)
- [演示脚本](docs/demo-script.md)
- [部署说明](docs/deployment.md)
- [首版发布清单](docs/release-checklist.md)
- [MySQL 建表脚本](mysql-schema-v1.sql)

## 发布清单

- `mvnw.cmd test` 全部通过。
- `scripts/smoke-test.ps1` 覆盖患者端导诊、挂号草稿、订单确认。
- `scripts/redis-hot-slot-loadtest.ps1` 验证热点号源无超卖。
- `.env.example` 不包含真实密钥。
- `target/`、`.env`、运行日志和本地数据不进入仓库。
