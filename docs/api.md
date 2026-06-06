# API 文档

统一响应格式:

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

## 系统状态

`GET /api/system/profile`

返回当前 profile、AI/向量/库存/事件 provider，以及 MySQL、MongoDB、Redis、Kafka、OpenAI、Pinecone 状态。

## 症状导诊

`POST /api/chat/diagnosis/stream`

`Content-Type: application/json`

```json
{
  "sessionId": "session-001",
  "hospitalId": "hospital-demo",
  "messages": [
    { "role": "user", "content": "最近三天发热咳嗽，晚上更明显。" }
  ],
  "consentToStoreHistory": true
}
```

返回 `text/event-stream`，事件包括 `meta`、`chunk`、`result`、`error`。

## 智能挂号导诊

`POST /api/chat/registration/stream`

请求体同上。挂号模式会额外执行 RAG 检索和排班匹配；如果匹配成功，`result.metadata.draft` 中包含待确认挂号草稿。

## 查询最新草稿

`GET /api/registration/draft/latest?sessionId=session-001`

用于患者端或冒烟脚本获取当前会话最新挂号草稿。

## 确认挂号

`POST /api/registration/confirm`

```json
{
  "draftId": "draft-id",
  "sessionId": "session-001",
  "idempotencyKey": "client-request-id",
  "patientName": "张三",
  "patientPhone": "13800000000",
  "idCard": "310101199001011234"
}
```

返回订单号、医生、排班、就诊日期和状态。重复确认同一草稿会返回同一订单，避免重复扣减库存。

## 查询订单

`GET /api/registration/orders?hospitalId=hospital-demo`

`GET /api/admin/orders?hospitalId=hospital-demo`

## 上传医院资料

`POST /api/admin/imports`

`multipart/form-data`

- `file`: 支持 CSV、Markdown 等项目导入服务已实现的格式。
- `hospitalId`: 可选，默认 `hospital-demo`。

返回导入任务 ID。任务异步处理，可通过导入任务列表查询状态。

## 导入任务列表

`GET /api/admin/imports?hospitalId=hospital-demo`

## 重试导入任务

`POST /api/admin/imports/{jobId}/retry`

## 重建向量索引

`POST /api/admin/vector/reindex?hospitalId=hospital-demo`

将 MySQL 中的知识分块重建到当前配置的向量库。
