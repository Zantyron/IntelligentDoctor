# Agent 与 RAG 实现说明

## Agent 范式

本项目采用 `ReAct-style tool-augmented RAG agent` 范式：

1. `Reason`：先通过 `AiGateway.analyze(...)` 对用户输入和历史上下文做结构化分诊分析。
2. `Act`：挂号模式下调用本地工具服务，包括 RAG 检索、科室查询、诊室查询、医生查询、排班查询和挂号草稿创建。
3. `Observe`：所有工具结果写入 MongoDB `tool_trace`，用于复盘每次工具调用。
4. `Answer`：将系统 Prompt、业务 Prompt、RAG 证据、工具规则和历史上下文拼接后发送给模型生成最终回复。

## RAG 链路

上传文档后，链路为：

1. 文档解析：`AdminImportService` 支持 CSV、Excel、Markdown、TXT、PDF。PDF 使用 PDFBox，Excel 使用 Apache POI。
2. 文档切割：`TextChunker` 按最大长度和 overlap 切割，尽量在句号、换行和分号处断开。
3. 向量化入库：`KnowledgeSearchService.rebuild(...)` 将 chunk 交给 `KnowledgeVectorStore`。`pinecone` 模式会调用 OpenAI-compatible embedding 后 upsert 到 Pinecone；`memory` 模式用于离线演示。
4. Query 处理：`KnowledgeSearchService.processQuery(...)` 会规范化问题，并按皮肤科、消化内科、儿科、耳鼻喉科、骨科等症状扩展检索词。
5. 粗排检索：`KnowledgeVectorStore.search(...)` 取 `limit * 4` 或至少 12 个候选。
6. Rerank 精排：`KnowledgeSearchService.rerank(...)` 用症状词覆盖和文本重合度对粗排候选重新计分。
7. Prompt 拼接：`PromptTemplateService.build(...)` 将精排证据放入 RAG Prompt，`OpenAiAiGateway.composeReply(...)` 再和系统/业务/工具 Prompt 共同发送给模型。

## AIService 与 Function Calling

代码层面使用 `AiGateway` 作为 AIService 边界，提供 `OpenAiAiGateway` 和 `RuleBasedAiGateway` 两种实现，便于真实模型和离线演示切换。

Function Calling 在当前版本采用“结构化工具调用 + 工具轨迹”的工程实现：

- `AgentToolService` 封装可调用业务函数：检索科室、诊室、医生、排班、挂号规则、创建挂号草稿。
- `ChatStreamResult.functionSuggestions` 返回建议调用的函数和参数。
- `ChatHistoryService.storeToolTrace(...)` 持久化工具名称、入参、结果。

后续如果要切换为 LangChain4j 原生 `AiServices` / `@Tool`，可以保留现有 `AgentToolService` 作为工具实现层，只替换 `OpenAiAiGateway` 的模型编排方式。
