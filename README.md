# PrivShield Java — 数据隐私计算 Java Agent

> **PrivShield Java** —— 企业级数据隐私计算 Java Agent，完整迁移自 [PrivShield Python](https://github.com/fengzhizi319/PrivShield)，提供 REST + gRPC 双协议高可用服务，支持数据脱敏、差分隐私、K-匿名化、查询混淆、医疗流水线及三层动态分类分级。
>
> 🌐 **GitHub Repository**: [https://github.com/fengzhizi319/PrivShield-java](https://github.com/fengzhizi319/PrivShield-java)

---

## 目录

- [架构概览](#架构概览)
- [快速开始](#快速开始)
- [核心功能](#核心功能)
- [API 参考](#api-参考)
- [构建与测试](#构建与测试)
- [配置说明](#配置说明)
- [文档](#文档)

---

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                    PrivShield Java                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │   Console    │    │    Agent     │    │   Privacy    │  │
│  │   (Web UI)   │───▶│   (Server)   │───▶│    (SDK)     │  │
│  │  React+Vite  │    │ gRPC + REST  │    │  算法核心层   │  │
│  │   :5174      │    │ :50051/:8079 │    │              │  │
│  └──────────────┘    └──────────────┘    └──────────────┘  │
│         │                    │                    │         │
│         ▼                    ▼                    ▼         │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐  │
│  │  Go Backend  │    │ DynClassify  │    │  Rules/ YAML │  │
│  │   (Proxy)    │    │   Engine     │    │  (配置层)     │  │
│  │   :8081      │    │  三层漏斗    │    │  domains/    │  │
│  └──────────────┘    └──────────────┘    │  standards/  │  │
│                                          │  taxonomies/ │  │
│                                          └──────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 项目结构

```
PrivShield-java/
├── agent/                    # Java Agent 主模块
│   ├── agent-sdk/            # 隐私计算 SDK
│   │   └── src/main/java/com/github/fengzhizi319/privacy/sdk/
│   │       ├── api/          # 核心 API (Masking/DP/KAnon/QoL)
│   │       ├── dynclassification/  # 动态分类分级引擎
│   │       ├── model/        # 数据模型
│   │       └── util/         # 工具类
│   ├── agent-server/         # gRPC + REST 服务端
│   │   └── src/main/java/com/privshield/agent/
│   │       ├── grpc/         # gRPC 服务实现 (33 个 RPC)
│   │       ├── rest/         # REST Controllers (13 个)
│   │       └── service/      # 业务编排层
│   ├── config/               # 配置文件
│   ├── rules/                # YAML 规则文件
│   ├── scripts/              # 启动脚本
│   └── Dockerfile            # Docker 构建
├── console/                  # 控制台模块
│   ├── backend-go/           # Go 后端 (gRPC 代理)
│   ├── web/                  # React 前端
│   └── scripts/              # 启动脚本集
└── privacy-java-sdk/         # 独立隐私计算库
```

---

## 快速开始

### 前置条件

- Java 17+
- Maven 3.8+
- Go 1.21+ (仅后端)
- Node.js 16+ (仅前端)

### 启动 Java Agent

```bash
cd agent

# 构建
mvn clean package -DskipTests

# 启动 (REST :8079 + gRPC :50051)
java -jar agent-server/target/agent-server-0.1.0-SNAPSHOT.jar
```

### 启动 Go 后端

```bash
cd console/backend-go

# 构建
go build -o server ./cmd/server

# 启动 (:8081)
./run.sh
```

### 启动前端

```bash
cd console/web

# 安装依赖
npm install

# 启动开发服务器 (:5174)
npm run dev
```

### 一键启动全部服务

```bash
# macOS / Linux
cd console/scripts
./dev-start-go.sh
```

---

## 核心功能

### 1. 数据脱敏 (Masking)

智能识别并掩码敏感信息：

```bash
curl -X POST http://localhost:8079/v1/privacy/mask \
  -H "Content-Type: application/json" \
  -d '{"value": "张三 13812345678 110101199001011234", "field_name": "mixed"}'

# 响应: {"result": "张**8"}
```

| 类型 | 原始值 | 脱敏结果 |
|------|--------|----------|
| 姓名 | 张三 | 张* |
| 手机 | 13812345678 | 138****5678 |
| 身份证 | 110101199001011234 | 110101********1234 |

### 2. 差分隐私 (Differential Privacy)

支持 Count/Sum/Mean 加噪查询：

```bash
# DP Count
curl -X POST http://localhost:8079/v1/privacy/dp/count \
  -H "Content-Type: application/json" \
  -d '{"values": [1,2,3,4,5], "epsilon": 1.0}'

# 响应: {"result": 4.5}  (真实值=5, 加 Laplace 噪声)
```

### 3. K-匿名化 (K-Anonymity)

记录级准标识符泛化：

```bash
curl -X POST http://localhost:8079/v1/privacy/k_anonymize/record \
  -H "Content-Type: application/json" \
  -d '{
    "record": {"name": "张三", "age": "30", "city": "北京"},
    "qi_cols": ["name", "age"],
    "k": 2
  }'
```

### 4. 查询混淆 (Query Obfuscation)

SQL 查询混淆 + 噪声注入：

```bash
curl -X POST http://localhost:8079/v1/privacy/qol/obfuscate \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT * FROM patients WHERE name=\"张三\"",
    "columns": ["name"]
  }'

# 响应: 原始查询 + 3 条噪声查询
```

### 5. 医疗流水线 (Medical Pipeline)

分类 + 脱敏一体化处理：

```bash
curl -X POST http://localhost:8079/v1/medical_pipeline/process_records \
  -H "Content-Type: application/json" \
  -d '{
    "records": [
      {"name": "张三", "phone": "13812345678", "diagnosis": "糖尿病"}
    ]
  }'

# 响应:
# {
#   "sanitized_data": [{"name": "张*", "phone": "138****5678", "diagnosis": "糖尿病"}],
#   "classification_report": [{"max_level": "L1"}]
# }
```

### 6. 动态分类分级 (DynClassification)

三层漏斗架构：规则引擎 → NER → LLM

```bash
curl -X POST http://localhost:8079/v1/dynclassification/eval \
  -H "Content-Type: application/json" \
  -d '{
    "field_name": "name",
    "value": "张三",
    "domain": "medical"
  }'

# 响应:
# {
#   "max_level": "L1",
#   "confidence": 0.95,
#   "engine_layer": "rule",
#   "tags": [{"level": "L1", "category": "个人信息", "rule_id": "medical.name"}]
# }
```

---

## API 参考

### gRPC 端点 (33 个 RPC)

| 方法 | 说明 |
|------|------|
| `Mask` | 数据脱敏 |
| `DPCount` / `DPSum` / `DPMean` | 差分隐私查询 |
| `KAnonymizeRecord` / `KAnonymizeTable` | K-匿名化 |
| `QolObfuscate` | 查询混淆 |
| `ClassifyField` / `ClassifyRecord` / `ClassifyTable` | 动态分类分级 |
| `Health` | 健康检查 |

### REST 端点 (13 个 Controller)

| 路径 | 方法 | 说明 |
|------|------|------|
| `/v1/privacy/mask` | POST | 数据脱敏 |
| `/v1/privacy/dp/count` | POST | DP 计数 |
| `/v1/privacy/dp/sum` | POST | DP 求和 |
| `/v1/privacy/dp/mean` | POST | DP 均值 |
| `/v1/privacy/k_anonymize/record` | POST | K-匿名记录 |
| `/v1/privacy/qol/obfuscate` | POST | 查询混淆 |
| `/v1/dynclassification/eval` | POST | 字段分类 |
| `/v1/dynclassification/eval_record` | POST | 记录分类 |
| `/v1/dynclassification/eval_table` | POST | 表分类 |
| `/v1/medical_pipeline/process_records` | POST | 医疗流水线 |
| `/health` | GET | 健康检查 |

---

## 构建与测试

### 构建 Agent

```bash
cd agent
mvn clean package -DskipTests
```

### 运行测试

```bash
cd agent
mvn test
```

### 构建 privacy-java-sdk

```bash
cd privacy-java-sdk
mvn clean package
```

### 运行 SDK 测试

```bash
cd privacy-java-sdk
mvn test
```

### Docker 构建

```bash
cd agent
docker build -t privshield-java-agent:latest .
```

---

## 配置说明

### Agent 配置

```yaml
# agent/config/application.yml
server:
  port: 8079          # REST 端口

grpc:
  server:
    port: 50051       # gRPC 端口

privacy:
  namespace: default
  rules-dir: rules    # YAML 规则目录
```

### YAML 规则文件

```
rules/
├── domains/          # 领域规则
│   ├── medical.yaml
│   ├── finance.yaml
│   └── general-pii.yaml
├── standards/        # 标准定义
│   ├── gd_health.yaml
│   └── jrt0197.yaml
└── taxonomies/       # 分类体系
    ├── default.yaml
    └── finance_jrt0197.yaml
```

---

## 文档

- [Agent 架构设计](agent/docs/design.md)
- [API 参考文档](agent/docs/api_reference.md)
- [运维指南](agent/docs/ops.md)
- [测试指南](agent/docs/testing.md)
- [privacy-java-sdk 文档](privacy-java-sdk/docs/)

---

## 技术栈

| 组件 | 技术 |
|------|------|
| Agent SDK | Java 17, privacy-java-sdk |
| Agent Server | Spring Boot 3.3.5, gRPC 1.62.2 |
| Go Backend | Go 1.21, Gin |
| Frontend | React 18, Vite, TypeScript, TailwindCSS |
| Build | Maven, Go Modules, npm |

---

## 许可证

Apache License 2.0

---

## 相关链接

- [PrivShield Python](https://github.com/fengzhizi319/PrivShield) — 原版 Python 实现
- [privacy-java-sdk](https://github.com/fengzhizi319/privacy-java-sdk) — 独立隐私计算库
- [privacy-go-sdk](https://github.com/fengzhizi319/privacy-go-sdk) — Go 语言隐私计算库
- [privacy-local-agent](https://github.com/fengzhizi319/privacy-local-agent) — Python 本地隐私 Agent
