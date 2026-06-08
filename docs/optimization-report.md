# IntelligentDoctor 项目全面优化分析报告

> 分析日期：2026-06-08 | 分支：master | 提交：6f3d41b (v0.1.0)

---

## 一、项目概况

| 维度 | 现状 |
|------|------|
| 框架 | Spring Boot 3.5.14 |
| Java | 17 |
| 构建 | Maven (mvnw) |
| 数据库 | MySQL 8.4 (11 张表) + MongoDB 7 (4 个集合) |
| 缓存/消息 | Redis 7 + Kafka 3.8 (KRaft) |
| AI | LangChain4j 1.15.1 (OpenAI 兼容协议) |
| 向量库 | Pinecone / InMemory |
| 架构 | 单体 Spring Boot，策略模式可插拔 |
| 前端 | 纯 HTML/CSS/JS 静态页面 |

---

## 二、严重问题 (Critical)

### 2.1 硬编码管理员凭据

**文件**：`src/main/java/com/intelligentdoctor/admin/security/AdminAuthInterceptor.java:15-16`

```java
private static final String USERNAME = "admin";
private static final String PASSWORD = "admin";
```

**问题**：管理员用户名密码直接硬编码在源码中，任何人阅读代码即可获取后台管理权限，且生产环境无法更换。

**建议**：改为通过 `@ConfigurationProperties` 从环境变量注入，并在生产环境强制设置。

---

### 2.2 挂号并发竞态导致号源泄漏

**文件**：`src/main/java/com/intelligentdoctor/registration/service/RegistrationService.java:67-108`

**问题**：`confirm()` 方法在检查幂等性和扣减号源之间存在竞态窗口。两次并发的 `confirm()` 调用可能：
1. 同时通过幂等性检查（都未发现已有订单）
2. 各自成功扣减号源（`slotStockService.reserve()`）
3. 第一个持久化订单成功，第二个因 `draftId` 唯一约束抛 `DataIntegrityViolationException`
4. 第二个的号源扣减**不会被释放**，造成永久性号源泄漏

**建议**：在 `confirm()` 入口处对 draft 实体加悲观锁（`@Lock(PESSIMISTIC_WRITE)`），或使用 Redis 分布式锁对 draftId 加锁。

---

### 2.3 忙轮询等待订单落库

**文件**：`src/main/java/com/intelligentdoctor/registration/service/RegistrationService.java:128-142`

```java
private RegistrationOrderEntity waitForOrderByDraftId(String draftId) {
    for (int i = 0; i < 20; i++) {
        RegistrationOrderEntity order = orderRepository.findByDraftId(draftId).orElse(null);
        if (order != null) { return order; }
        Thread.sleep(100);
    }
    throw ...;
}
```

**问题**：
- 阻塞 Web 线程最多 2 秒执行 20 次数据库轮询
- `@Transactional` 在此轮询期间保持数据库连接/事务打开，可能导致连接池耗尽
- Kafka 模式下消费者可能尚未收到消息，导致频繁超时

**建议**：改用事件驱动的异步回调机制，对于本地提供者使用 `ApplicationEventPublisher` + `@EventListener`，对于 Kafka 提供者使用 `CompletableFuture` + 回调。

---

### 2.4 生产环境连接池未配置

**文件**：`src/main/resources/application-prod.yml`

**问题**：生产配置中完全没有 HikariCP、Redis Lettuce、MongoDB 连接池参数，全部使用默认值：

| 池 | 默认最大值 | 生产风险 |
|----|-----------|---------|
| HikariCP | 10 连接 | 并发请求稍高即耗尽 |
| Lettuce Redis | 8 连接 | 号源扣减高峰期阻塞 |
| MongoDB Driver | 100 连接 | 尚可，但无超时配置 |

**建议**：在 `application-prod.yml` 中显式配置连接池参数：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 5000
      leak-detection-threshold: 10000
  redis:
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
```

---

### 2.5 `.env` 文件包含真实 API 密钥

**文件**：`.env`

**问题**：`.env` 包含明文 OpenAI API Key (`sk-cb...`) 和 Pinecone API Key (`pcsk_...`)。通过 `spring.config.import: optional:file:.env[.properties]` 加载后可能经由 Actuator 端点或错误响应泄露。

**建议**：
- 立即轮换已泄露的密钥
- 从 `.env` 中移除真实密钥，仅在 `.env.example` 保留模板
- 生产环境使用 Kubernetes Secrets / Vault / 云密钥管理服务
- 确保 `.env` 已加入 `.gitignore`

---

### 2.6 缺少限流保护

**问题**：整个项目没有任何限流机制。`/api/chat/diagnosis/stream` 和 `/api/chat/registration/stream` 端点可被无限调用，导致 AI API 配额迅速耗尽。

**建议**：引入 `bucket4j` 或 Spring Cloud Gateway 限流，对 AI 调用端点按 IP/会话维度限制 QPS。

---

## 三、高优先级问题 (High)

### 3.1 缺少应用级缓存

**问题**：虽然引入了 Redis，但只用于号源库存，没有配置任何应用缓存（`@EnableCaching`、`@Cacheable` 均未使用）。以下高频查询每次都打到数据库：

- 科室列表查询 (`CatalogQueryService`)
- 诊室列表查询
- 医生列表查询
- 排班时段查询
- 挂号规则查询
- 知识分块查询

**建议**：
- 添加 `@EnableCaching`
- 配置 `RedisCacheManager`，对目录数据设置 TTL 缓存（如 30 分钟）
- 在 `CatalogQueryService` 的查询方法上添加 `@Cacheable`

---

### 3.2 `loadClinicIds` 和 `loadDoctorIds` 全表加载

**文件**：`src/main/java/com/intelligentdoctor/admin/service/AdminImportService.java:474-488`

```java
clinicRoomRepository.findAll()  // 加载全部诊室到内存
    .stream().filter(c -> hospitalId.equals(c.getHospitalId()))
```

`DepartmentRepository.loadDepartmentIds()` 正确使用了 `findByHospitalId` 查询，但诊室和医生查询是 `findAll()` + 内存过滤。多医院部署下会加载数万条记录到内存。

**建议**：在 `ClinicRoomRepository` 中添加 `findByHospitalId`，在 `DoctorRepository` 中添加 `findByHospitalId` 方法。

---

### 3.3 导入操作缺少事务边界

**文件**：`src/main/java/com/intelligentdoctor/admin/service/AdminImportService.java`

**问题**：`importStructuredRows()`（第 277 行）和 `importTextualKnowledge()`（第 336 行）没有 `@Transactional` 注解。每一步 `save()` 都是独立提交，如果中途失败，已导入的数据不会回滚。

**建议**：在 `importStructuredRows()` 和 `importTextualKnowledge()` 上添加 `@Transactional`，确保失败时全部回滚。

---

### 3.4 MongoDB 查询全集合扫描

**文件**：`docker/mongodb/init/01-indexes.js`

**问题**：虽然初始化脚本定义了 4 个索引，但覆盖不完整。以下查询场景缺少索引：

| 集合 | 缺失索引 | 影响查询 |
|------|---------|---------|
| `chat_message` | `sessionId + createdAt` 复合索引 | `findBySessionIdOrderByCreatedAtAsc` 全表扫描 |
| `prompt_trace` | `sessionId` 索引 | `findBySessionId` / `deleteBySessionId` 全表扫描 |
| `tool_trace` | `sessionId` 索引 | 同上 |

**建议**：为 `chat_message`、`prompt_trace`、`tool_trace` 添加 `{sessionId: 1, createdAt: 1}` 复合索引。

---

### 3.5 `storeChat` 逐条保存消息

**文件**：`src/main/java/com/intelligentdoctor/chat/history/ChatHistoryService.java:137-151`

**问题**：每条消息单独调用 `messageRepository.save()`，N 条消息产生 N 次 MongoDB 网络往返。

**建议**：改用 `messageRepository.saveAll(messages)` 批量写入。

---

### 3.6 `deleteAllSessions` 中的 N+1 删除

**文件**：`src/main/java/com/intelligentdoctor/chat/history/ChatHistoryService.java:107-111`

```java
for (ChatSessionDocument session : sessions) {
    deleteSession(session.getSessionId()); // 内部执行 4 次删除
}
```

对于 N 个会话，产生 4N+1 次数据库操作。且没有事务保护，中途崩溃会导致数据残留。

**建议**：使用批量删除：`messageRepository.deleteAllBySessionIdIn(sessionIds)` + `promptTraceRepository.deleteAllBySessionIdIn(sessionIds)` 等。

---

### 3.7 `server.error.include-message: always`

**文件**：`src/main/resources/application.yml:36`

**问题**：错误响应中总是包含详细错误消息，在生产环境会泄露堆栈跟踪、SQL 语句和内部架构信息。

**建议**：生产配置中改为 `never` 或 `on-param`。

---

### 3.8 缺少数据库迁移工具

**问题**：项目有 3 份 SQL schema 文件（`mysql-schema-v1.sql`、`docs/mysql-schema-v1.sql`、`docker/mysql/init/01-schema.sql`），但无 Flyway 或 Liquibase。生产环境使用 `ddl-auto: validate`，schema 变更需手动执行，极易出错。

**建议**：引入 Flyway，将所有 DDL 迁移到版本化脚本中。

---

### 3.9 `upsertSchedule` 总是创建新记录

**文件**：`src/main/java/com/intelligentdoctor/admin/service/AdminImportService.java:397`

**问题**：`upsertDepartment`、`upsertClinic`、`upsertDoctor` 都有"先查后改"逻辑，但 `upsertSchedule` 总是 `new ScheduleSlotEntity()` + `save()`。重复导入 CSV 会产生重复的排班记录。

**建议**：为 `upsertSchedule` 添加按（hospitalId, doctorId, slotDate, period）查重逻辑。

---

### 3.10 `SseEmitter` 无限超时

**文件**：`src/main/java/com/intelligentdoctor/chat/service/ChatOrchestratorService.java:61`

```java
SseEmitter emitter = new SseEmitter(0L); // 0 = 永不过期
```

**问题**：客户端断开后，emitter 永不超时释放，可能积累僵尸连接耗尽资源。

**建议**：设置合理超时（如 5 分钟），并注册 `onCompletion`/`onTimeout`/`onError` 回调清理资源。

---

### 3.11 AI 响应中泄露完整 Prompt 到客户端

**文件**：`src/main/java/com/intelligentdoctor/ai/provider/OpenAiAiGateway.java:173-178`

**问题**：metadata 中包含完整 `promptTrace`（system prompt + business prompt + RAG prompt + tool prompt），随每个 SSE 事件返回客户端，暴露内部 RAG 策略且浪费带宽。

**建议**：从 SSE metadata 中移除 `promptTrace`，仅保留在服务端日志/PromptTrace 文档中。

---

### 3.12 JSON 解析 LLM 输出不够健壮

**文件**：`src/main/java/com/intelligentdoctor/ai/provider/OpenAiAiGateway.java:222-233`

**问题**：`extractJson()` 通过查找首尾 `{` `}` 来提取 JSON，且未使用 OpenAI 的 `response_format: {type: "json_object"}` 来强制结构化输出。模型输出格式漂移可能静默破坏解析。

**建议**：
- 在 ChatCompletionRequest 中设置 `response_format: {type: "json_object"}`
- 添加 JSON Schema 校验，解析失败时使用明确的降级策略

---

### 3.13 `OpenAiAiGateway` 在构造器中急切创建 ChatModel

**文件**：`src/main/java/com/intelligentdoctor/ai/provider/OpenAiAiGateway.java:42-49`

**问题**：当 `RuleBasedAiGateway` 激活（`app.ai.provider=rule-based`）时，如果 API key 未配置，`OpenAiAiGateway` 仍会在启动时尝试构建 `OpenAiChatModel`，导致应用启动失败。

**建议**：将 `ChatModel` 创建改为懒加载，或使用 `@ConditionalOnProperty` 确保仅在 `openai` 提供者激活时才创建 Bean。

---

## 四、中优先级问题 (Medium)

### 4.1 症状-科室映射散落在 3 处

| 位置 | 方法 |
|------|------|
| `RuleBasedAiGateway` | `analyze()` 中的关键词匹配 |
| `KnowledgeSearchService` | `processQuery()` 中的 `expandKeywords()` |
| `OpenAiAiGateway` | `fallbackAnalysis()` 的降级逻辑 |

**建议**：将映射提取为配置（YAML/数据库），统一为 `SymptomDepartmentMappingService`，三处调用同一服务。

---

### 4.2 CatalogQueryService 内存过滤

**文件**：`src/main/java/com/intelligentdoctor/catalog/service/CatalogQueryService.java:86-92`

**问题**：`resolveDepartmentByName()` 加载医院所有科室后在 Java 中用 `String.contains()` 做模糊匹配。当科室数量增长时效率低。

**建议**：改为数据库级 `LIKE` 查询：

```java
@Query("SELECT d FROM DepartmentEntity d WHERE d.hospitalId = ?1 AND d.name LIKE %?2%")
List<DepartmentEntity> findByNameContaining(String hospitalId, String name);
```

---

### 4.3 MySQL 索引不足

以下高频查询缺少最优索引：

| 表 | 查询 | 建议索引 |
|----|------|---------|
| `department` | `findByHospitalIdOrderBySortOrderAscNameAsc` | `(hospital_id, sort_order, name)` |
| `clinic_room` | `findByHospitalIdAndDepartmentId` | `(hospital_id, department_id)` |
| `doctor` | `findByHospitalIdAndClinicRoomId` | `(hospital_id, clinic_room_id)` |
| `schedule_slot` | `findByHospitalIdAndDepartmentIdAndSlotDate...` | `(hospital_id, department_id, slot_date)` |
| `schedule_slot` | `findByHospitalIdAndDoctorIdAndSlotDate...` | `(hospital_id, doctor_id, slot_date)` |
| `registration_rule` | `findByHospitalIdAndDepartmentId` | `(hospital_id, department_id)` |
| `registration_order` | `findByHospitalIdOrderByCreatedAtDesc` | `(hospital_id, created_at)` |
| `registration_draft` | `findBySessionIdOrderByCreatedAtDesc` | `(session_id, created_at)` |

---

### 4.4 MySQL 缺少外键约束

数据库中所有表都没有 `FOREIGN KEY` 约束。例如 `department.hospital_id` 引用 `hospital.id` 但在数据库层面无法保证引用完整性。建议至少为核心关系添加外键。

---

### 4.5 实体缺少 `@Version` 乐观锁

所有 JPA 实体都没有 `@Version` 字段。在并发场景（如号源扣减）中可能导致"丢失更新"问题。虽然 Redis Lua 脚本提供了库存原子性，但订单/草稿的并发更新仍需乐观锁保护。

---

### 4.6 实体缺少 `equals`/`hashCode`

所有 JPA 实体和 MongoDB 文档都未实现 `equals()` 和 `hashCode()`。默认的 `Object` 实现基于对象标识，在跨事务、缓存或集合操作时可能产生错误行为。

**建议**：在 `BaseEntity` 中基于 `id` 实现：

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof BaseEntity that)) return false;
    return id != null && id.equals(that.id);
}

@Override
public int hashCode() {
    return id != null ? id.hashCode() : super.hashCode();
}
```

---

### 4.7 MongoDB 时间戳管理不一致

| 文档 | createdAt | updatedAt |
|------|-----------|-----------|
| `ChatSessionDocument` | 手动设置 | 手动设置 |
| `ChatMessageDocument` | 手动设置 | 无此字段 |
| `PromptTraceDocument` | 手动设置 | 无此字段 |
| `ToolTraceDocument` | 手动设置 | 无此字段 |

**建议**：统一添加 `@CreatedDate` / `@LastModifiedDate` 注解，启用 MongoDB 审计自动填充。

---

### 4.8 线程池参数硬编码

**文件**：`src/main/java/com/intelligentdoctor/config/AppConfig.java:50-52`

```java
executor.setCorePoolSize(6);
executor.setMaxPoolSize(12);
executor.setQueueCapacity(200);
```

**建议**：提取为 `@ConfigurationProperties`：

```yaml
app:
  executor:
    core-pool-size: 6
    max-pool-size: 12
    queue-capacity: 200
```

---

### 4.9 重试机制已启用但未使用

`@EnableRetry` 存在于主类，但整个项目未使用任何 `@Retryable` 注解。AI 调用和库存操作应添加重试逻辑处理瞬态故障。

---

### 4.10 依赖版本未显式锁定

`mysql-connector-j`、MongoDB Driver 等关键依赖的版本完全由 Spring Boot BOM 继承。建议在 `pom.xml` 中显式锁定关键依赖版本，并定期扫描 CVE。

---

### 4.11 `sessionTitle()` 加载全部消息

**文件**：`src/main/java/com/intelligentdoctor/chat/history/ChatHistoryService.java:188-195`

```java
List<ChatMessageDocument> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
// 遍历找第一条用户消息
```

**建议**：添加仓库方法 `findFirstBySessionIdAndRoleOrderByCreatedAtAsc(sessionId, "USER")`，或只在会话文档中存储 `title` 字段避免每次计算。

---

### 4.12 `LocalSlotStockService` 全局 synchronized 锁

**文件**：`src/main/java/com/intelligentdoctor/registration/stock/LocalSlotStockService.java:24-48`

**问题**：`synchronized` 方法锁住整个服务实例，同一时刻只有一个线程能操作库存，12 个线程池线程中 11 个将阻塞等待。

**建议**：使用 `ConcurrentHashMap` 实现按 slotId 分段锁，或统一使用 Redis Lua 方案。

---

### 4.13 `importTextualKnowledge` 中删-存-重建非原子操作

**文件**：`src/main/java/com/intelligentdoctor/admin/service/AdminImportService.java:336-341`

并发导入同一 source name 时，两线程可能先后执行 delete，然后都执行 save 和 rebuild，产生重复分块。

**建议**：添加分布式锁（Redis）保护同一 hospitalId + sourceName 的导入操作。

---

### 4.14 Kafka 配置不完整

`application.yml` 中 Kafka 配置仅设置了序列化器，缺少：

- `acks: all`（生产可靠性）
- `retries: 3`（生产重试）
- `enable.auto-commit: false`（消费者手动提交）
- `max-poll-records` 限制
- `listener.concurrency` 并发数

---

### 4.15 `waitForOrderByDraftId` 异常抛出模糊

```java
throw new RuntimeException("Order not persisted in time for draft: " + draftId);
```

应该抛出自定义业务异常，包含 draftId、等待时间、使用的提供者类型，便于排查。

---

## 五、低优先级问题 (Low)

### 5.1 缺少结构化日志

无 Logback 配置，无 JSON 格式日志输出。生产环境建议引入 `logstash-logback-encoder` 输出 JSON 格式日志，便于 ELK/Datadog 采集。

---

### 5.2 缺少 Prometheus 指标

Actuator 仅暴露 `health` 和 `info`，未引入 `micrometer-registry-prometheus`。建议添加并暴露关键业务指标：

- AI 调用延迟和成功率
- 号源库存操作计数
- 挂号成功/失败计数
- 线程池队列深度

---

### 5.3 缺少 CORS 配置

无 `addCorsMappings` 配置。如果前后端分离部署在不同域名下，会导致浏览器跨域请求被阻止。

---

### 5.4 缺少 Staging/UAT Profile

项目只有 `dev`、`test`、`prod` 三个 Profile，缺少预发布环境配置。

---

### 5.5 `verify-release.ps1` 检查错误文件

发行验证脚本扫描 `.env.example` 而非 `.env` 来检测泄露的密钥，导致真实的 `.env` 密钥从未被检查。

---

### 5.6 `schedule_slot.period` 缺少约束

`period` 字段为原始 `VARCHAR(32)`，无枚举或 `CHECK` 约束。建议改为 Java `enum` 并在数据库添加 `CHECK` 约束（`period IN ('MORNING', 'AFTERNOON')`）。

---

### 5.7 `consultation_fee` 单位不明确

`INT` 类型，无法确定是以"分"还是"元"为单位。建议在字段名或注释中明确单位。

---

### 5.8 文件上传允许任意扩展名

**文件**：`AdminImportService.java:551-553`

上传文件保留原始扩展名存储到服务器文件系统，未限制文件类型。建议白名单仅允许 `.csv`、`.xlsx`、`.pdf`。

---

### 5.9 缺少数据库 TTL 清理

`registration_draft` 有过期机制但无定时清理任务。`chat_session`、`chat_message` 等 MongoDB 集合无 TTL 索引，历史数据将无限增长。

---

## 六、架构层面建议

### 6.1 依症状科室映射集中管理

当前散落在 `RuleBasedAiGateway`、`KnowledgeSearchService`、`OpenAiAiGateway` 三处的症状-科室映射应统一为配置驱动：

```yaml
app:
  symptom-mappings:
    - symptoms: [发烧, 发热, 体温高]
      departments: [发热门诊, 内科]
    - symptoms: [咳嗽, 咳痰, 气喘]
      departments: [呼吸内科]
```

### 6.2 订单落库改为完全事件驱动

`waitForOrderByDraftId` 的轮询模式应替换为：

```
confirm() -> 扣减库存 -> 发布事件 -> 返回 202 Accepted（不再等待）
消费者收到事件 -> 持久化订单 -> 更新 draft 状态
前端通过 SSE 或轮询订单状态获取最终结果
```

### 6.3 引入数据库迁移工具（Flyway/Liquibase）

替代当前手工管理的多份 SQL 文件，将所有 DDL 变更纳入版本控制。

### 6.4 提取共享配置到公共 YAML

当前 `application-dev.yml` 和 `application-prod.yml` 存在大量配置重复（JPA、Jackson、Server 等）。建议提取到 `application.yml` 的共同段。

---

## 七、优化优先级汇总

| 优先级 | 编号 | 问题 | 影响 |
|--------|------|------|------|
| **P0** | 2.2 | 挂号竞态号源泄漏 | 数据正确性 |
| **P0** | 2.5 | .env 含真实密钥 | 安全性 |
| **P0** | 2.1 | 硬编码管理员凭据 | 安全性 |
| **P0** | 2.4 | 生产连接池未配置 | 可用性 |
| **P0** | 2.6 | 缺少限流 | 可用性/成本 |
| **P1** | 3.3 | 导入缺少事务 | 数据一致性 |
| **P1** | 3.2 | findAll 全表加载 | 性能 |
| **P1** | 3.1 | 缺少应用缓存 | 性能 |
| **P1** | 3.4 | MongoDB 缺索引 | 性能 |
| **P1** | 3.10 | SseEmitter 无线超时 | 资源泄漏 |
| **P1** | 3.12 | JSON 解析不健壮 | 功能正确性 |
| **P1** | 3.13 | 启动时急切创建 ChatModel | 可用性 |
| **P1** | 3.7 | 错误信息泄露 | 安全性 |
| **P2** | 4.3 | MySQL 索引不足 | 性能 |
| **P2** | 4.2 | 内存过滤 | 性能 |
| **P2** | 4.6 | 缺 equals/hashCode | 代码健壮性 |
| **P2** | 4.8 | 线程池硬编码 | 可运维性 |
| **P2** | 4.9 | 重试未使用 | 可用性 |
| **P2** | 3.9 | upsertSchedule 重复 | 数据正确性 |
| **P3** | 5.x | 日志/监控/清理等 | 可运维性 |

---

*报告由 Claude Code 自动生成，建议按优先级逐项处理。*
