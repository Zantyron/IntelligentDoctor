# Intelligent Doctor

智能导诊与挂号系统。项目以 Spring Boot 为后端核心，内置患者端和管理后台，围绕“症状咨询、RAG 知识召回、智能推荐科室/诊室/医生、挂号草稿确认、号源预占、订单落库、后台知识导入”构建完整闭环。

这个项目适合作为 Java 后端、AI 应用、RAG 工程化、医疗业务系统方向的项目经验展示。

## 核心亮点

- **大模型流式输出**：患者端使用 SSE 接收响应，后端对 OpenAI-compatible Chat API 使用 `stream=true`，模型生成 token 后实时推送到前端。
- **RAG 检索链路**：支持文档解析、文本切割、Embedding、向量入库、Query 处理、向量粗排、词法 rerank 精排和 Prompt 拼接。
- **智能挂号推荐**：根据症状和上下文推荐科室，并把可预约诊室、医生、日期、时段、剩余号源和费用以表格形式展示。
- **聊天记忆存储**：支持多轮上下文记忆、新开对话、历史会话保留、单条删除和全部删除，数据库记录同步处理。
- **Function Calling / Tool 层**：封装科室检索、诊室查询、医生查询、排班查询、挂号草稿创建等工具能力，便于 Agent 编排。
- **挂号一致性保障**：支持实名信息填写、草稿确认、Redis Lua 号源预占、本地/Kafka 事件落库和订单幂等处理。
- **管理后台**：支持后台登录、资料导入、导入任务状态、知识库重建、系统依赖状态查看。
- **工程化交付**：提供 Docker Compose、初始化 SQL、冒烟测试、压测脚本、发布检查清单和完整项目文档。

## 技术栈

| 类型 | 技术 |
| --- | --- |
| 后端 | Java 17, Spring Boot 3.5, Spring MVC, Validation |
| 数据库 | MySQL, MongoDB, H2 Test Profile |
| 缓存/消息 | Redis, Redis Lua, Kafka |
| AI | LangChain4j, OpenAI-compatible Chat API, Embedding API |
| RAG | 文档切割, 向量化, InMemory/Pinecone Vector Store, rerank |
| 前端 | HTML, CSS, JavaScript, SSE Stream Reader |
| 测试 | JUnit 5, Spring Boot Test, Awaitility |
| 工程 | Docker Compose, Maven Wrapper, PowerShell Scripts |

## 系统能力

### 患者端

- 症状导诊和智能挂号两种模式。
- 大模型逐字/逐段流式输出。
- 等待态、生成态、错误态完整反馈。
- 智能推荐可展开/收起，默认收起。
- 可预约号源表格展示诊室、医生、时间段和余号。
- 挂号草稿默认隐藏，生成草稿后再出现。
- 挂号成功后展示醒目的成功弹窗。
- 用户消息和 AI 消息支持复制、删除。
- 支持新开对话，同时保留旧对话。

### 管理后台

- 患者端跳转后台前需要登录。
- 默认账号密码为 `admin/admin`，可通过环境变量覆盖。
- 支持上传 CSV、Markdown、TXT、PDF、Excel 等资料。
- 导入后生成知识切片，并可重建向量库。
- 查看导入任务状态和系统依赖状态。

### RAG 与 Agent 流程

当前 Agent 范式为 `react-rag-tool`：

1. 接收用户输入和历史上下文。
2. 进行意图识别和 Query 处理。
3. 检索医院知识库，先向量粗排。
4. 对候选知识片段进行 rerank 精排。
5. 调用工具查询科室、诊室、医生和排班。
6. 拼接系统 Prompt、业务 Prompt、历史摘要、RAG 证据和工具结果。
7. 调用大模型生成回复，并通过 SSE 实时输出到前端。

## 项目结构

| 目录 | 说明 |
| --- | --- |
| `src/main/java/com/intelligentdoctor/admin` | 管理后台、资料导入、导入任务状态和后台鉴权。 |
| `src/main/java/com/intelligentdoctor/ai` | 大模型网关、Prompt 编排、AIService 边界和工具调用建议。 |
| `src/main/java/com/intelligentdoctor/chat` | 患者端聊天、SSE 流式响应、聊天记忆和会话删除。 |
| `src/main/java/com/intelligentdoctor/knowledge` | RAG 知识库，包含文档切割、向量化、检索和重排。 |
| `src/main/java/com/intelligentdoctor/registration` | 挂号草稿、实名信息、号源预占、订单生成和事件处理。 |
| `src/main/java/com/intelligentdoctor/catalog` | 医院主数据，包括科室、诊室、医生、排班和规则。 |
| `src/main/java/com/intelligentdoctor/system` | 系统状态探活、运行 profile 和启动地址输出。 |
| `src/main/resources/static` | 患者端和管理后台静态页面。 |
| `docker` | MySQL、MongoDB、Redis、Kafka 本地依赖配置。 |
| `sample-data` | 演示医院、医生、排班和病症知识库样例。 |
| `scripts` | 启动、冒烟测试、压测和发布检查脚本。 |
| `docs` | 架构、接口、部署和演示文档。 |

## 快速启动

准备 Java 17 和 Docker Desktop。

复制配置模板：

```powershell
Copy-Item .env.example .env
```

启动本地依赖：

```powershell
.\scripts\start-deps.ps1 -WithKafka
```

启动应用：

```powershell
.\scripts\start-app.ps1 -JavaHome "C:\Program Files\Java\jdk-17"
```

也可以使用离线演示模式，不依赖真实云端模型和向量库：

```powershell
.\scripts\run-demo.ps1 -JavaHome "C:\Program Files\Java\jdk-17"
```

启动后访问：

- 患者端：[http://localhost:8080/](http://localhost:8080/)
- 管理后台：[http://localhost:8080/admin.html](http://localhost:8080/admin.html)
- 系统状态：[http://localhost:8080/api/system/profile](http://localhost:8080/api/system/profile)

## 常用配置

`.env.example` 提供完整模板。正式环境建议至少配置：

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
ADMIN_USERNAME=admin
ADMIN_PASSWORD=change-me
```

不要提交 `.env`、真实 API Key、数据库密码或云服务密钥。

## 验收命令

```powershell
.\mvnw.cmd test
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
.\scripts\redis-hot-slot-loadtest.ps1 -Stock 50 -Concurrency 200
```

Redis 压测结果中的 `noOversell` 应为 `true`，表示热点号源没有超卖。

## 文档

- [架构说明](docs/architecture.md)
- [RAG 与 Agent 设计](docs/agent-rag-design.md)
- [接口文档](docs/api.md)
- [部署说明](docs/deployment.md)
- [演示脚本](docs/demo-script.md)
- [发布清单](docs/release-checklist.md)
- [MySQL 建表脚本](mysql-schema-v1.sql)

## 安全说明

- `.env` 已加入 `.gitignore`。
- `.env.example` 只保留占位符，不包含真实密钥。
- 管理后台默认账号仅用于演示，生产环境必须通过环境变量修改。
- 真实 API Key 如曾暴露，应立即在平台侧轮换。

## 项目定位

这是一个面向演示和项目经验展示的智能医疗业务系统，重点体现：

- Java 后端业务建模能力。
- 大模型应用接入能力。
- RAG 工程化落地能力。
- Redis/Kafka 在挂号场景中的一致性设计。
- 前后端完整闭环交付能力。
