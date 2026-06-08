# com.intelligentdoctor

后端核心代码目录，采用 Spring Boot 单体结构，按业务能力拆分模块。

| 模块 | 说明 |
| --- | --- |
| `admin` | 管理后台、资料导入、导入任务状态和后台鉴权。 |
| `ai` | 大模型接入、Prompt 编排、AIService 边界和工具调用建议。 |
| `catalog` | 医院主数据，包括科室、诊室、医生、排班和规则。 |
| `chat` | 患者端聊天、SSE 流式响应、聊天记忆和会话删除。 |
| `knowledge` | RAG 知识库，包含文档切割、向量化、检索和重排。 |
| `registration` | 挂号草稿、实名信息、号源预占、订单生成和事件处理。 |
| `system` | 系统状态探活、运行 profile 和启动地址输出。 |
| `common` | 通用响应、异常处理、JSON 和 SSE 工具。 |
| `config` | 应用配置属性、拦截器、线程池和跨域配置。 |
| `infrastructure` | 基础持久化模型和公共基础设施代码。 |
