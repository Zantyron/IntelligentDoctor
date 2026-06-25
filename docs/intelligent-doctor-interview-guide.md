# 智能导诊挂号系统面试讲解文档

> 适用场景：面试中介绍简历里的“智能导诊挂号系统”项目。
> 核心目标：把“我做了一个 AI 导诊项目”讲成“我完整落地了 Agent + RAG + Tool Calling + 挂号一致性 + 可观测链路”。

## 1. 项目一句话介绍

智能导诊挂号系统是一个基于 Spring Boot 的 AI 医疗导诊与预约挂号系统，用户用自然语言描述症状后，系统会完成症状理解、风险提示、科室推荐、知识库证据召回、医生和排班查询、挂号草稿生成、患者确认、号源预占和订单落库，形成从咨询到挂号的端到端闭环。

面试中可以这样说：

> 这个项目不是单纯调用大模型聊天，而是把大模型能力接到真实业务链路里。后端先对患者症状做结构化分诊分析，再通过 RAG 召回医院知识库证据，结合本地工具查询科室、医生、排班和挂号规则，最后生成挂号草稿。用户确认后，通过 Redis Lua 保证号源扣减原子性，并通过事件机制完成订单落库，同时用 MongoDB 记录会话、Prompt 和 Tool Trace，方便排查 AI 链路问题。

## 2. 技术栈与模块映射

简历写法：

> Spring Boot + LangChain4j + RAG + Redis + LLM API + MySQL + MongoDB

项目中的真实映射：

| 能力 | 对应实现 |
| --- | --- |
| 后端框架 | Spring Boot 3.5, Java 17 |
| AI 接入 | `AiGateway`, `OpenAiAiGateway`, `RuleBasedAiGateway` |
| LangChain4j Agent | `LangChain4jTriageAgentRuntime`, `LangChain4jTriageAgent`, `@Tool` 工具 |
| 原生 Agent 编排 | `NativeTriageAgentRuntime`, `ChatOrchestratorService` |
| RAG 检索 | `AdminImportService`, `TextChunker`, `KnowledgeSearchService`, `KnowledgeVectorStore` |
| 向量库 | Pinecone 生产适配，InMemory 本地演示适配 |
| Tool Calling | `AgentToolService`, `KnowledgeSearchTool` |
| 流式响应 | `ChatController` + `SseEmitter` + OpenAI-compatible stream |
| 挂号链路 | `RegistrationService`, `RegistrationController` |
| 号源一致性 | `RedisLuaSlotStockService`, `LocalSlotStockService` |
| 异步解耦 | `KafkaRegistrationEventPublisher`, `KafkaRegistrationEventConsumer` |
| 会话与 Trace | `ChatHistoryService` + MongoDB 文档 |
| 主业务数据 | MySQL: 科室、诊室、医生、排班、规则、草稿、订单、知识分块 |

## 3. 整体业务流程

可以按这条主线讲：

1. 用户在患者端选择“症状导诊”或“智能挂号”，输入症状描述。
2. `ChatController` 接收请求，通过 SSE 建立流式响应。
3. `ChatOrchestratorService` 获取当前医院租户、创建流式任务、限制并发。
4. Agent Runtime 合并历史会话，调用 `AiGateway.analyze(...)` 做结构化分诊分析。
5. `KnowledgeSearchService.search(...)` 根据症状摘要检索医院知识库，得到 Top-K 证据。
6. `PromptTemplateService` 拼接系统角色、业务规则、RAG 证据和工具规则。
7. 大模型生成回答，后端通过 SSE 把 token 或分片实时推给前端。
8. 智能挂号模式下，系统根据推荐科室调用工具查询诊室、医生和排班。
9. 找到可预约号源后，自动创建挂号草稿。
10. 用户补齐实名信息并确认挂号。
11. `RegistrationService.confirm(...)` 校验草稿、预占号源、发布挂号事件。
12. Redis 模式下用 Lua 脚本原子扣减号源，Kafka 模式下异步消费事件并落订单。
13. MongoDB 记录会话、RAG 检索、Prompt Trace 和 Tool Trace，方便复盘问题。

## 4. Agent 链路怎么讲

简历写法：

> 基于 LangChain4j 构建智能导诊 Agent，设计“症状理解 - 意图识别 - 置信度判断 - 科室推荐 - 工具调用 - 挂号草稿生成”编排链路，引入低置信度反问澄清机制，实现自然语言咨询到预约挂号的端到端闭环。

面试展开：

这个 Agent 的核心不是让模型直接给最终答案，而是把一次导诊拆成几个可控步骤：

1. 症状理解：用 `AiGateway.analyze(...)` 把用户自然语言转换成结构化 `TriageAnalysis`，包括症状摘要、可能方向、紧急程度、推荐科室、风险提示和抽取槽位。
2. 意图识别：系统区分 `DIAGNOSIS` 和 `REGISTRATION` 两种模式。诊断模式偏健康建议和风险提示，挂号模式会继续走医生、排班和草稿生成。
3. 置信度与澄清：当前项目没有单独定义数值型 `confidence` 字段，而是通过“信息是否缺失、是否出现急症信号、模型输出的 cautionNotes、extractedSlots 是否完整”判断是否需要追问。Prompt 中明确要求信息不足时先追问关键症状和挂号信息。
4. 科室推荐：模型先给出 suggestedDepartments，后端再用 `CatalogQueryService.resolveDepartmentByName(...)` 映射到系统里的真实科室。
5. 工具调用：挂号模式下调用 `AgentToolService` 查询科室、诊室、医生、排班和挂号规则。
6. 草稿生成：如果找到可预约号源，`ChatOrchestratorService#createDraftFromRecommendation(...)` 自动生成待确认挂号草稿。

可以补充一句：

> 这样设计的原因是医疗场景不能让模型直接“拍脑袋”确定挂号结果，模型负责理解和生成解释，业务系统负责查真实医生、排班和库存。

## 5. Prompt 分层设计怎么讲

简历写法：

> 针对医疗场景安全性与可解释性要求，设计系统角色层、业务规则层、上下文注入层三层 Prompt 结构，将症状描述、历史会话、风险提示与 RAG 证据统一注入模型上下文，并通过结构化输出约束降低超范围诊断等高风险回答。

项目实现：

`PromptTemplateService` 把 Prompt 拆成几层：

| 层级 | 内容 | 作用 |
| --- | --- | --- |
| 系统角色层 | “只能提供导诊、分诊、挂号建议，不能确诊、开药或替代医生面诊” | 限制医疗安全边界 |
| 业务规则层 | 诊断模式和挂号模式的不同输出要求 | 让模型知道当前任务 |
| RAG 证据层 | 检索到的医院知识库片段 | 给回答提供依据 |
| 工具规则层 | 挂号前必须补齐实名信息、日期、时段等 | 约束业务流程 |
| 时间上下文 | 当前医院业务时间，只推荐未来号源 | 避免推荐已过期排班 |

结构化输出主要体现在 `OpenAiAiGateway.analyze(...)` 中，要求模型返回 JSON：

```json
{
  "symptomSummary": "主诉摘要",
  "possibleConditions": ["可能方向"],
  "urgencyLevel": "LOW|MEDIUM|HIGH",
  "suggestedDepartments": ["推荐科室"],
  "cautionNotes": ["风险提醒"],
  "extractedSlots": {
    "duration": "持续时间",
    "temperature": "体温",
    "redFlags": "危险信号"
  }
}
```

面试重点：

> 我没有让模型直接自由发挥，而是先让模型输出结构化分诊结果，再基于这个结果进行 RAG、工具调用和最终回复生成。这样前端展示更稳定，也能减少模型超范围诊断。

## 6. RAG 知识增强链路怎么讲

简历写法：

> 搭建 RAG 知识增强链路，覆盖文档解析、语义分块、Embedding 入库、向量召回与关键词重排全流程，沉淀科室说明、医生信息、挂号规则等知识片段，Top-5 证据注入后提升复杂症状场景回答依据覆盖率。

项目实现链路：

1. 后台上传资料：`AdminImportService` 支持 CSV、Excel、Markdown、TXT、PDF。
2. 文档解析：PDF 使用 PDFBox，Excel 使用 Apache POI，CSV 使用 Commons CSV。
3. 文本分块：`TextChunker` 按最大长度和 overlap 切分，优先在句号、换行、分号处分割，减少语义截断。
4. 知识落库：分块保存到 MySQL 的知识分块表。
5. 向量重建：`KnowledgeSearchService.rebuild(...)` 读取知识分块并写入 `KnowledgeVectorStore`。
6. 向量库适配：生产可走 Pinecone，本地演示可走 InMemory。
7. Query 处理：`processQuery(...)` 会做标点清洗，并根据症状扩展科室关键词，例如腹痛扩展“消化内科”、皮疹扩展“皮肤科”。
8. 粗排召回：向量库先召回 `limit * 4` 或至少 12 条候选。
9. 关键词重排：`rerank(...)` 用症状词覆盖和文本重合度给候选加分。
10. Top-5 证据注入 Prompt：让模型回答时引用医院知识，而不是只靠通用经验。

需要注意的口径：

当前代码里 `AdminImportService#createChunks(...)` 使用的是 `420 / 60`，而简历里写的是 `chunk 512 / overlap 64`。如果面试官追问非常细，建议按当前项目说：

> 项目最初设计目标是 512 / 64，当前代码里为了中文内容和演示数据做了更保守的 420 / 60。这个参数本质上可以配置化，主要取决于模型上下文、知识片段粒度和召回质量。

## 7. Tool Calling 工具层怎么讲

简历写法：

> 设计统一 Tool Calling 工具层，将科室检索、医生查询、排班查询、挂号规则查询、挂号草稿创建等业务能力抽象为标准化 Tool Schema，支持模型基于用户意图自主编排工具调用顺序。

项目实现：

`AgentToolService` 封装了 6 类业务工具：

| Tool | 作用 |
| --- | --- |
| `searchDepartments` | 根据症状或关键词检索科室 |
| `searchClinics` | 查询科室下的诊室 |
| `searchDoctors` | 查询科室和诊室下的医生 |
| `querySchedules` | 查询医生可预约排班 |
| `queryRegistrationRules` | 查询科室挂号规则 |
| `createRegistrationDraft` | 创建待患者确认的挂号草稿 |

这些方法都加了 LangChain4j 的 `@Tool` 和 `@P` 注解，`LangChain4jTriageAgentRuntime` 通过 `AiServices.builder(...).tools(...)` 把它们注册给 Agent。

同时项目也保留了 native runtime：

| Runtime | 特点 |
| --- | --- |
| `native` | 后端显式编排分析、RAG、工具查询、草稿生成，稳定可控 |
| `langchain4j` | 使用 LangChain4j `AiServices`、`TokenStream`、`MessageWindowChatMemory` 和 `@Tool` |

面试中可以这样讲：

> 我把工具层和模型编排层解耦了。工具本质是稳定的业务能力，模型只负责决定什么时候需要什么信息。即使后续从 native runtime 切到 LangChain4j 原生 Agent，也不用重写科室、医生、排班这些业务查询逻辑。

## 8. SSE 流式响应怎么讲

用户体验上，AI 应用不能等几十秒后一次性返回，所以系统使用 SSE 做流式输出。

项目流程：

1. `ChatController` 的 `/api/chat/diagnosis/stream` 和 `/api/chat/registration/stream` 返回 `SseEmitter`。
2. `ChatOrchestratorService.stream(...)` 先发送 `meta` 事件，告诉前端模式、医院和 Runtime。
3. 模型生成过程中发送 `chunk` 事件，前端逐段显示。
4. 完成后发送 `result` 事件，里面包含 summary、possibleConditions、recommendations、evidence、functionSuggestions 和 metadata。
5. 异常时发送 `error` 事件。

项目还做了并发保护：

`StreamConcurrencyLimiter` 会限制单医院并发流式请求数量，避免模型接口慢调用拖垮服务线程池。

面试表达：

> SSE 相比 WebSocket 更轻量，适合这种服务端单向持续推送 token 的场景。后端用 SseEmitter 封装事件，前端只需要监听 meta、chunk、result、error 几类事件即可。

## 9. 挂号一致性与 Redis Lua 怎么讲

简历写法：

> 针对专家号并发预约场景，基于 Redis Lua 脚本将号源校验与扣减封装为原子操作，结合预约 Token、防重复提交与失败回滚机制，保障高并发场景下号源一致性，并引 Kafka 异步解耦通知与日志写入，避免主链路阻塞。

项目里的核心问题：

专家号库存很少，但并发点击确认挂号可能很多。如果只用普通的“查库存 -> 判断 -> 扣减”，多个请求可能同时读到库存大于 0，导致超卖。

解决方案：

1. 用户确认挂号时，先根据草稿 ID 查草稿，并使用数据库锁避免同一个草稿并发修改。
2. 检查该草稿是否已有订单，如果已有，直接返回已有订单，保证幂等。
3. 调用 `SlotStockService.reserve(...)` 预占号源。
4. Redis 模式下，`RedisLuaSlotStockService` 用 Lua 脚本把 `GET`、库存判断、`DECRBY` 放到一个原子操作里。
5. 预占成功后生成 reservation token，并写入 Redis，设置 30 分钟过期。
6. 如果后续发布事件或落订单失败，调用 `release(...)` 回滚库存。
7. Kafka 模式下发布 `RegistrationReservedEvent`，消费者异步创建订单。
8. 订单落库时根据 draftId 做幂等判断，避免重复消费生成多笔订单。

Lua 预占逻辑可以这样解释：

```lua
local current = tonumber(redis.call('GET', KEYS[1]) or '-1')
local quantity = tonumber(ARGV[1])
if current < 0 then
    return -2
end
if current < quantity then
    return -1
end
redis.call('DECRBY', KEYS[1], quantity)
return current - quantity
```

面试重点：

> Redis Lua 的价值是把库存校验和扣减变成 Redis 单线程里的原子执行，避免并发条件下多个请求同时通过库存校验。数据库层再通过草稿锁和订单唯一性兜底，保证同一个草稿重复确认不会重复扣库存。

## 10. Kafka 异步解耦怎么讲

项目支持本地事件和 Kafka 事件两种模式。

核心类：

| 类 | 作用 |
| --- | --- |
| `KafkaRegistrationEventPublisher` | 挂号预占成功后发送事件 |
| `KafkaRegistrationEventConsumer` | 消费事件并调用订单落库服务 |
| `RegistrationOrderPersistenceService` | 幂等创建订单，更新草稿状态 |

为什么要引入 Kafka：

1. 挂号确认主链路不直接承担所有后置逻辑。
2. 后续可以扩展短信通知、日志审计、支付初始化等消费者。
3. 消费失败可重试，订单落库用幂等逻辑兜底。

回答时注意：

当前代码里的 `KafkaRegistrationEventPublisher` 使用 `.join()` 等待发送成功，所以严格来说它不是“完全异步返回客户端后再慢慢处理”，而是“用事件模型把库存预占和订单落库解耦，订单创建由消费者处理”。如果面试官追问得很细，可以这样说：

> 当前为了演示一致性，生产者等待 Kafka send 成功，避免事件丢失；真正生产环境可以进一步引入 outbox 或可靠消息表，做到更完整的最终一致性。

## 11. MongoDB 可观测与 Trace 怎么讲

简历写法：

> 基于 MongoDB 设计 AI 调用链路可观测能力，记录会话上下文、Prompt Trace、RAG 检索记录与 Tool 调用 Trace，通过 traceId 串联导诊分析、知识召回、工具调用与挂号确认全流程，支持问题定位与复盘。

项目实现：

`ChatHistoryService` 负责存储：

| 数据 | 说明 |
| --- | --- |
| `ChatSessionDocument` | 会话维度信息 |
| `ChatMessageDocument` | 用户消息和 AI 回复 |
| `PromptTraceDocument` | Prompt 内容 |
| `ToolTraceDocument` | 工具名、入参、返回结果 |

当前代码主要用 `sessionId + hospitalId` 串联一次会话链路，Tool Trace 里记录了 `ragSearch`、`searchDoctors`、`querySchedules`、`confirmRegistration` 等工具调用。

面试中可以这样讲：

> AI 项目上线后最怕回答不稳定、检索不到证据、工具入参错误但无法复盘。所以我把会话、Prompt、RAG 检索和工具调用结果都落到 MongoDB。出现问题时，可以按 sessionId 查到用户原始输入、模型上下文、召回了哪些知识、调用了哪些工具以及最终创建了哪个挂号草稿。

如果面试官追问 traceId：

> 当前项目以 sessionId 作为主串联 ID，部分工具 Trace 会记录业务 ID，比如 draftId、slotId 和 token。后续可以进一步增加 requestId/traceId，让一次流式请求、RAG、Tool 和订单事件之间的链路更标准化。

## 12. 数据模型怎么讲

项目里可以分三类数据：

### 12.1 医院主数据

存在 MySQL：

- 医院
- 科室
- 诊室
- 医生
- 排班
- 挂号规则

这些数据由后台导入，挂号时通过 `CatalogQueryService` 查询。

### 12.2 AI 知识数据

存在 MySQL + 向量库：

- MySQL 保存知识 chunk 原文、来源、metadata。
- Pinecone 保存向量和 metadata，用于语义检索。
- InMemory 用于离线演示和测试。

### 12.3 挂号交易数据

存在 MySQL + Redis：

- MySQL 保存挂号草稿和挂号订单。
- Redis 保存热点号源库存和 reservation token。
- Kafka 传递挂号预占成功事件。

### 12.4 会话与观测数据

存在 MongoDB：

- 聊天会话
- 聊天消息
- Prompt Trace
- Tool Trace

## 13. 面试时 2 分钟版本

可以直接背这个版本：

> 我这个项目是一个智能导诊挂号系统，主要解决患者不知道该挂什么科、医院导诊和挂号链路割裂的问题。技术上用 Spring Boot 做后端，AI 部分接入 OpenAI 兼容接口和 LangChain4j，业务上打通了症状咨询、RAG 证据召回、科室医生排班查询、挂号草稿生成和确认挂号。
>
> 用户输入症状后，系统先通过 `AiGateway.analyze` 让模型输出结构化分诊结果，包括症状摘要、可能方向、紧急程度和推荐科室。然后 `KnowledgeSearchService` 根据症状摘要去医院知识库做 RAG 检索，先向量粗排，再做关键词重排，取 Top-5 证据注入 Prompt。Prompt 我分成系统安全层、业务规则层、RAG 证据层和工具规则层，明确要求不能确诊、不能开药，信息不足要追问。
>
> 挂号模式下，我把科室检索、诊室查询、医生查询、排班查询、挂号规则和草稿创建封装成 Tool，LangChain4j runtime 下可以通过 `@Tool` 调用，native runtime 下由后端显式编排。找到可预约号源后会自动生成挂号草稿，用户补齐实名信息后确认挂号。
>
> 并发一致性方面，专家号源用 Redis Lua 做原子校验和扣减，避免超卖；同一个草稿重复确认会先查已有订单，保证幂等；预占成功后通过本地事件或 Kafka 事件触发订单落库。可观测方面，MongoDB 保存会话、Prompt Trace、RAG 检索和 Tool Trace，方便排查模型回答和工具调用问题。

## 14. 面试高频追问与回答

### Q1：为什么要用 RAG，不直接让大模型回答？

因为医院导诊需要结合本院实际科室、医生、排班和挂号规则。大模型只靠通用知识可能推荐不存在的科室或医生。RAG 可以把本院知识片段注入上下文，让回答有依据，也方便展示 evidence，提升可解释性。

### Q2：RAG 的召回质量怎么优化？

我做了三层优化：

1. 文档切分时尽量按句号、换行和分号断句，减少语义被切断。
2. Query 处理时根据症状扩展科室关键词，例如“腹痛”扩展“消化内科”，“皮疹”扩展“皮肤科”。
3. 向量召回后再做 lexical rerank，对症状词和文本重合度高的片段加分。

### Q3：医疗场景怎么降低模型风险？

主要从三方面控制：

1. 系统 Prompt 明确禁止确诊、开药和替代医生面诊。
2. 结构化分析中抽取 urgencyLevel 和 cautionNotes，遇到胸痛、呼吸困难、意识障碍等急症信号优先建议急诊。
3. 业务结果不完全交给模型决定，真实科室、医生、排班、库存都由后端工具查询。

### Q4：Function Calling 和普通接口调用有什么区别？

普通接口调用是后端固定流程；Function Calling 是把业务能力抽象成工具，让 Agent 根据用户意图决定是否调用、调用哪个、用什么参数。本项目里工具层是 `AgentToolService`，既能给 LangChain4j `@Tool` 使用，也能被 native 编排复用。

### Q5：为什么保留 native runtime 和 LangChain4j runtime 两套？

native runtime 稳定、可控，适合演示和生产兜底；LangChain4j runtime 更接近原生 Agent，可以用 `AiServices`、`TokenStream`、`MessageWindowChatMemory` 和 `@Tool`。两者共用同一套工具服务和 RAG 服务，避免业务逻辑重复。

### Q6：怎么防止号源超卖？

Redis Lua 把库存读取、判断和扣减放在同一个脚本里原子执行。应用层还做了草稿锁和订单幂等判断。如果订单落库前失败，会根据 reservation token 回滚库存。

### Q7：重复点击确认挂号怎么办？

`RegistrationService.confirm(...)` 会先按 hospitalId + draftId 查已有订单。如果订单已存在，直接返回同一个 orderNo，不再扣库存。测试类 `RegistrationConsistencyTests` 验证了重复确认只扣一次库存。

### Q8：MongoDB 存 Trace 有什么价值？

AI 链路问题很难只靠日志排查。MongoDB 记录了用户输入、AI 回复、Prompt、RAG 检索和工具调用。比如推荐错科室，可以查到是症状分析错了、RAG 召回错了，还是工具查询结果不足。

### Q9：如果向量库或模型服务不可用怎么办？

项目里有两类降级：

1. 向量库可切换 InMemory，用于本地演示和测试。
2. AI 网关有 `RuleBasedAiGateway`，在非 openai provider 下可以用规则兜底，保证基本导诊流程可跑。

### Q10：项目里最有技术含量的点是什么？

可以回答：

> 我认为最有价值的是把不确定的大模型能力和确定的医疗业务系统结合起来。模型负责理解症状和生成解释，RAG 提供医院知识依据，Tool 查询真实业务数据，Redis Lua 和数据库幂等保证挂号一致性，MongoDB Trace 保证问题可复盘。这个项目体现的是 AI 工程化落地，而不是单纯调接口。

## 15. 简历口径建议

为了面试时更稳，建议注意下面几个点：

1. “低置信度反问澄清”不要说成已经有复杂模型打分，可以说是基于信息完整度、危险信号和结构化槽位缺失触发追问。
2. “traceId 串联全流程”当前代码更准确是 sessionId + 业务 ID 串联；如果被追问，说明后续会增加 requestId/traceId。
3. “chunk 512 / overlap 64”与当前代码 `420 / 60` 不完全一致，建议统一成“chunk 参数可配置，当前演示实现为 420 / 60”。
4. “Kafka 异步解耦”当前生产者等待 send 成功，准确说是事件模型解耦订单落库；生产级可以继续做 outbox。
5. “多轮导诊命中率 88%”如果被问数据来源，建议说是基于模拟问诊样例的人工评估结果，评估口径是 Top-1/Top-3 推荐科室是否覆盖预期科室。

## 16. 可以反向展示的工程亮点

面试官如果让你挑一个点深入，推荐按优先级选：

1. Redis Lua 防超卖：后端面试最容易讲清楚，也能体现并发一致性。
2. RAG 链路：AI 应用岗最看重，能体现文档解析、chunk、embedding、召回、rerank、证据注入。
3. Tool Calling：能体现 Agent 工程化，不是简单聊天。
4. Prompt 分层和医疗安全：能体现业务理解和风险意识。
5. Mongo Trace：能体现可观测性和上线排障意识。

## 17. 代码阅读入口

按这几个文件复习即可：

| 入口 | 文件 |
| --- | --- |
| SSE 主链路 | `src/main/java/com/intelligentdoctor/chat/service/ChatOrchestratorService.java` |
| 原生 Agent Runtime | `src/main/java/com/intelligentdoctor/chat/agent/NativeTriageAgentRuntime.java` |
| LangChain4j Runtime | `src/main/java/com/intelligentdoctor/chat/agent/langchain4j/LangChain4jTriageAgentRuntime.java` |
| Prompt 分层 | `src/main/java/com/intelligentdoctor/ai/prompt/PromptTemplateService.java` |
| AI 网关 | `src/main/java/com/intelligentdoctor/ai/provider/OpenAiAiGateway.java` |
| 工具层 | `src/main/java/com/intelligentdoctor/ai/tools/AgentToolService.java` |
| RAG 检索 | `src/main/java/com/intelligentdoctor/knowledge/service/KnowledgeSearchService.java` |
| 文档导入 | `src/main/java/com/intelligentdoctor/admin/service/AdminImportService.java` |
| 挂号确认 | `src/main/java/com/intelligentdoctor/registration/service/RegistrationService.java` |
| Redis Lua | `src/main/java/com/intelligentdoctor/registration/stock/RedisLuaSlotStockService.java` |
| 订单落库 | `src/main/java/com/intelligentdoctor/registration/service/RegistrationOrderPersistenceService.java` |
| 会话与 Trace | `src/main/java/com/intelligentdoctor/chat/history/ChatHistoryService.java` |

## 18. 面试收尾表达

可以这样总结：

> 这个项目对我最大的锻炼是，我不只是把 LLM 接进来，而是围绕真实挂号业务做了一套可控的 AI 应用架构。RAG 解决知识依据问题，Tool Calling 解决真实业务数据查询问题，Prompt 分层解决医疗安全边界问题，Redis Lua 和幂等设计解决高并发一致性问题，Mongo Trace 解决 AI 链路可观测问题。所以它更像一个 AI + 后端业务系统的完整落地项目。

