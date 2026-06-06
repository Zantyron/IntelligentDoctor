# 演示脚本

## 准备

1. 执行 `Copy-Item .env.example .env`，按需填入 OpenAI、Embedding 和 Pinecone 配置。
2. 执行 `.\scripts\start-deps.ps1 -WithKafka`。
3. 执行 `.\scripts\start-app.ps1 -JavaHome "C:\Program Files\Java\jdk-17"`。
4. 打开 `http://localhost:8080/api/system/profile`，确认 MySQL、MongoDB、Redis、Kafka 为 `up`。

## 患者端导诊

1. 打开 `http://localhost:8080/`。
2. 选择症状导诊。
3. 输入: `最近三天发热咳嗽，晚上更明显。`
4. 观察 SSE 流式输出，讲解症状归纳、建议科室和风险提示。

## 智能挂号

1. 切换到智能挂号。
2. 输入: `胸闷心慌，想挂心内科专家号。`
3. 观察系统召回心内科知识片段，匹配心内科专家号源，并生成挂号草稿。
4. 填入患者信息并确认挂号。
5. 到管理后台订单列表确认订单生成。

## 后台资料导入

1. 打开 `http://localhost:8080/admin.html`。
2. 上传 `sample-data/hospital-import.csv` 或 `sample-data/hospital-knowledge.md`。
3. 查看导入任务状态从 `PENDING` 到 `COMPLETED`。
4. 点击重建向量索引。
5. 回到患者端重新提问，说明 RAG 依据来自医院知识库。

## 压测展示

执行:

```powershell
.\scripts\redis-hot-slot-loadtest.ps1 -Stock 50 -Concurrency 200
```

讲解输出:

- `success`: 成功预占数量，不能超过初始库存。
- `insufficient`: 库存不足的请求数。
- `remaining`: Redis 剩余库存。
- `noOversell`: 必须为 `true`。

## 自动化验收

执行:

```powershell
.\mvnw.cmd test
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```

冒烟脚本会覆盖系统状态、症状导诊 SSE、挂号 SSE、草稿查询和订单确认。
