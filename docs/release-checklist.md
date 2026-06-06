# 首版发布清单

## 代码与配置

- `README.md` 可从零引导启动。
- `.env.example` 不包含真实密钥。
- `.gitignore` 排除 `.env`、`target/`、`data/`、日志和 pid 文件。
- `pom.xml` 使用 Java 17，并显式声明 UTF-8 编码。
- `mvnw.cmd` 在 Windows PowerShell 下可启动 Maven Wrapper。

## 自动化验证

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

期望:

- 单元测试通过。
- 集成测试通过。
- RAG 检索测试通过。
- 挂号一致性测试通过。
- Live 验证测试没有配置外部条件时允许跳过。

## 联调验证

```powershell
.\scripts\start-deps.ps1 -WithKafka
.\scripts\start-app.ps1 -JavaHome "C:\Program Files\Java\jdk-17"
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```

无外部依赖的演示兜底:

```powershell
.\scripts\run-demo.ps1 -JavaHome "C:\Program Files\Java\jdk-17"
.\scripts\smoke-test.ps1 -BaseUrl http://localhost:8080
```

期望:

- 患者端可打开。
- 管理后台可打开。
- `/api/system/profile` 返回依赖状态。
- 症状导诊 SSE 返回 `result` 事件。
- 智能挂号 SSE 生成草稿。
- 挂号确认生成订单。

## Redis 热点号源压测

```powershell
.\scripts\redis-hot-slot-loadtest.ps1 -Stock 50 -Concurrency 200
```

期望:

- `success` 不超过 `initialStock`。
- `remaining` 不小于 0。
- `success + remaining == initialStock`。
- `noOversell == true`。

## 发布动作

当前目录必须先成为 Git 仓库或放入已有 Git 工作树，然后执行:

```powershell
git status --short
git add README.md docs scripts src pom.xml .env.example .gitignore docker-compose.yml mvnw.cmd
git commit -m "release: prepare intelligent doctor v0.1.0"
git tag v0.1.0
```

如果仓库托管在远端:

```powershell
git push origin HEAD
git push origin v0.1.0
```

发布前不要提交 `.env`、`data/`、`target/` 或真实密钥。
