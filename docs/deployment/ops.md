# PrivShield Java — 部署与运维手册 (Operations Manual)

> 本文档为 **PrivShield Java** 数据隐私计算治理边车服务及其控制台系统的全栈部署与运维指南。覆盖本地开发、多组分自动化脚本、Docker Compose 容器编排、Kubernetes 原生部署、Helm Chart 生产发布、安全配置与可观测性监控。

---

## 目录

- [1. 系统架构与服务拓扑](#1-系统架构与服务拓扑)
- [2. 环境准备与依赖规范](#2-环境准备与依赖规范)
- [3. 镜像构建与打包](#3-镜像构建与打包)
- [4. Console 自动化运维脚本集 (`console/scripts/`)](#4-console-自动化运维脚本集-consolescripts)
  - [4.1 开发模式 (`dev-*.sh`)](#41-开发模式-dev-sh)
  - [4.2 生产静态托管模式 (`prod-*.sh`)](#42-生产静态托管模式-prod-sh)
  - [4.3 Docker 容器编排模式 (`docker-*.sh`)](#43-docker-容器编排模式-docker-sh)
  - [4.4 启停管理与 PID 安全清理](#44-启停管理与-pid-安全清理)
- [5. Docker Compose 全栈编排 (`deploy/docker-compose/`)](#5-docker-compose-全栈编排-deploydocker-compose)
  - [5.1 服务清单与网络拓扑](#51-服务清单与网络拓扑)
  - [5.2 常用命令速查](#52-常用命令速查)
  - [5.3 Profile 机制与 LLM 解耦](#53-profile-机制与-llm-解耦)
- [6. Kubernetes 原生部署 (`deploy/k8s/`)](#6-kubernetes-原生部署-deployk8s)
- [7. Helm 生产发布 (`deploy/helm/`)](#7-helm-生产发布-deployhelm)
- [8. 安全配置 (mTLS / API Key / 限流)](#8-安全配置-mtls--api-key--限流)
- [9. 健康检查与探针体系](#9-健康检查与探针体系)
- [10. 监控告警与可观测性 (Prometheus + Grafana)](#10-监控告警与可观测性-prometheus--grafana)
- [11. 常见故障排查 (Troubleshooting)](#11-常见故障排查-troubleshooting)

---

## 1. 系统架构与服务拓扑

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PrivShield Java 部署拓扑                         │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────┐             ┌─────────────────────────────┐   │
│   │     Console Web     │             │    Console Backend (Go)     │   │
│   │    (React + Vite)   │────────────▶│       (gRPC 代理网关)        │   │
│   │     :5173 / :5174   │             │            :8081            │   │
│   └─────────────────────┘             └──────────────┬──────────────┘   │
│              │                                       │ (gRPC :50051)    │
│              │ (REST :8080 备选)                     │                  │
│              ▼                                       ▼                  │
│   ┌─────────────────────┐             ┌─────────────────────────────┐   │
│   │ Console Backend(Py) │────────────▶│    PrivShield Java Agent    │   │
│   │   (FastAPI REST)    │ (REST:8079) │    (Spring Boot 3.3.5)      │   │
│   │        :8080        │             │   REST: 8079 / gRPC: 50051  │   │
│   └─────────────────────┘             └──────────────┬──────────────┘   │
│                                                      │                  │
│                                       ┌──────────────┴──────────────┐   │
│                                       │     Dynamic Classification  │   │
│                                       │  L1 Rules → L2 NER → L3 LLM │   │
│                                       └──────────────┬──────────────┘   │
│                                                      │ (HTTP :8000)     │
│                                                      ▼                  │
│                                       ┌─────────────────────────────┐   │
│                                       │   vLLM GPU 推理 (可选独立)   │   │
│                                       │            :8000            │   │
│                                       └─────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 端口分配规范

| 组件 | 协议 | 默认端口 | 环境变量 | 说明 |
|---|---|---|---|---|
| **Java Agent REST** | HTTP | `8079` | `PRIVACY_REST_PORT` | Spring Boot REST 接口、Actuator `/actuator/health` |
| **Java Agent gRPC** | gRPC | `50051` | `PRIVACY_GRPC_PORT` | 33 个核心隐私计算与动态分类 gRPC 方法 |
| **Console Go Backend** | HTTP | `8081` | `PRIVACY_CONSOLE_PORT` | Go gRPC 代理网关，支持直接托管前端 `dist` |
| **Console Python Backend** | HTTP | `8080` | `PRIVACY_CONSOLE_PORT` | FastAPI REST 代理后端（备选通道） |
| **Console Web UI (Dev)** | HTTP | `5173` | - | Vite HMR 开发服务器 |
| **Console Web UI (Prod)** | HTTP | `5174` / `8081` | - | Nginx 容器独立暴露或 Go 后端直接静态托管 |
| **vLLM 推理服务** | HTTP | `8000` | `LLM_API_BASE` | OpenAI 兼容接口，Layer-3 大模型分类平滑 |
| **Prometheus** | HTTP | `9090` | - | 时序指标采集器 |
| **Grafana** | HTTP | `3000` | - | 可视化监控仪表盘 |

---

## 2. 环境准备与依赖规范

| 工具 / 运行时 | 最低版本 | 推荐版本 | 用途 |
|---|---|---|---|
| **OpenJDK** | 17 | 17.0.8+ (Eclipse Temurin / CRaC) | Java Agent 编译与运行 |
| **Apache Maven** | 3.8.0 | 3.9.6+ | Java 项目多模块构建 |
| **Go** | 1.21 | 1.24+ | Console Go 代理后端 |
| **Node.js & npm** | 16.14 (npm 8+) | Node 18+ / 20+ (pnpm 9+) | Web 前端打包与开发服务器 |
| **Docker** | 20.10+ | 24.0+ (含 BuildKit) | 容器镜像构建 |
| **Docker Compose** | v2.0+ | v2.24+ | 单机全栈容器编排 |
| **Kubernetes** | 1.24+ | 1.28+ | 生产容器集群编排 |
| **Helm** | 3.10+ | 3.12+ | K8s 应用包管理与发布 |

---

## 3. 镜像构建与打包

### 3.1 Java Agent 镜像构建

`agent/Dockerfile` 采用多阶段构建，第一阶段由 `maven:3.9-eclipse-temurin-17-alpine` 完成依赖下载与 fat JAR 打包，第二阶段基于精简的 `eclipse-temurin:17-jre-alpine` 运行时镜像交付：

```bash
# 在 agent 目录下构建 Java Agent 镜像
cd agent
docker build -t privshield-java-agent:0.1.0 .

# 本地运行测试
docker run -d --name privshield-agent \
  -p 8079:8079 -p 50051:50051 \
  privshield-java-agent:0.1.0
```

### 3.2 Go 后端代理镜像构建

`console/backend-go/Dockerfile` 采用 `golang:1.23-alpine` 静态编译 (`CGO_ENABLED=0`)，产物注入 `alpine:3.20` 最小运行时镜像（~15MB）：

```bash
cd console/backend-go
docker build -t privacy-console-backend-go:0.1.0 .
```

### 3.3 前端 Nginx 镜像构建

```bash
cd console/web
docker build -t privacy-console-web:0.1.0 .
```

---

## 4. Console 自动化运维脚本集 (`console/scripts/`)

`console/scripts/` 目录提供了覆盖开发、生产与容器化全场景的一键式运维脚本。

### 4.1 开发模式 (`dev-*.sh`)

开发模式会启动前端 Vite HMR 开发服务器（端口 `5173`），支持 `<50ms` 实时模块热重载：

```bash
# 1. 启动 Go gRPC 代理模式（推荐主力开发环境）
# 启动组件：Java Agent (:8079/:50051) + Go Backend (:8081) + Vite UI (:5173)
./console/scripts/dev-start-go.sh

# 2. 启动双后端模式（可在前端顶部自由切换 Python / Go 后端）
# 启动组件：Java Agent + Python Backend (:8080) + Go Backend (:8081) + Vite UI (:5173)
./console/scripts/dev-start-all.sh

# 3. 启动 Python REST 代理模式
./console/scripts/dev-start.sh

# 4. 启动 Go gRPC mTLS 双向加密认证模式
# 自动在 console/backend-go/certs/ 生成测试证书链并开启双向 TLS 认证
./console/scripts/dev-start-go-mtls.sh

# Windows PowerShell 开发启动
powershell -ExecutionPolicy Bypass -File console\scripts\dev-start-go.ps1
```

> **参数提示**：`dev-start*.sh` 均支持 `--force` 参数，在 CI 或非交互式环境中自动清理占用端口的旧进程。

### 4.2 生产静态托管模式 (`prod-*.sh`)

生产模式会自动预先编译构建前端静态资源（`console/web/dist`），由 Go / Python 后端直接挂载提供静态 UI，无需运行 Node.js 进程：

```bash
# 1. 启动 Go gRPC 静态托管生产环境（控制台 UI & API 统一在 :8081）
./console/scripts/prod-start-go.sh

# 2. 启动双后端静态托管生产环境 (Python :8080 | Go :8081)
./console/scripts/prod-start-all.sh

# 3. 启动 Python REST 静态托管生产环境 (UI & API :8080)
./console/scripts/prod-start.sh

# 4. 启动 mTLS 双向认证静态托管生产环境
./console/scripts/prod-start-go-mtls.sh
```

> **重新编译参数**：添加 `--rebuild` 可强制清除前端 dist 产物并重新触发 Java/Go 编译打包。

### 4.3 Docker 容器编排模式 (`docker-*.sh`)

基于 Docker Compose 快速编排多容器微服务：

```bash
# 1. 启动 Java Agent + Go 代理 + React Web UI 容器套件
./console/scripts/docker-start-go.sh

# 2. 启动 Java Agent + Python 代理 + React Web UI 容器套件
./console/scripts/docker-start-python.sh

# 3. 启动全栈基础套件（Agent + 双后端 + Web UI）
./console/scripts/docker-start-all.sh

# 4. 启动全栈套件 + vLLM 大模型 GPU 推理容器
./console/scripts/docker-start-all.sh --with-llm

# 5. 一键优雅停止并清理所有容器与网络
./console/scripts/docker-stop.sh
```

### 4.4 启停管理与 PID 安全清理

脚本集内置了严格的 PID 与端口两段式清理机制（先 `SIGTERM` 优雅关闭，超时则 `SIGKILL` 强杀）：

```bash
# 安全停止所有开发模式服务（释放 5173, 8080, 8081, 8079, 50051 端口）
./console/scripts/dev-stop.sh

# 安全停止所有生产模式服务
./console/scripts/prod-stop.sh
```

---

## 5. Docker Compose 全栈编排 (`deploy/docker-compose/`)

### 5.1 服务清单与网络拓扑

配置文件位于 `deploy/docker-compose/docker-compose.yml`：

- **PrivShield**：Java Agent 主服务（Spring Boot），暴露 `8079` (REST) 与 `50051` (gRPC)。
- **console-backend-go**：Go gRPC 代理后端，暴露 `8081`。
- **console-backend-python**：Python FastAPI 代理后端，暴露 `8080`。
- **console-web**：Nginx 托管的前端 Web UI，暴露 `5173`。
- **vllm**（可选，`--profile llm`）：GPU 加速大模型推理服务，暴露 `8000`。
- **prometheus**（可选，`--profile monitoring`）：Prometheus 监控服务，暴露 `9090`。
- **grafana**（可选，`--profile monitoring`）：Grafana 监控看板，暴露 `3000`。

### 5.2 常用命令速查

```bash
cd deploy/docker-compose

# 启动核心服务
docker compose up -d

# 启动核心服务 + GPU 大模型推理
docker compose --profile llm up -d

# 启动核心服务 + Prometheus & Grafana 监控栈
docker compose --profile monitoring up -d

# 启动全部服务（核心 + LLM + 监控）
docker compose --profile llm --profile monitoring up -d

# 查看容器运行状态
docker compose ps

# 查看实时聚合日志
docker compose logs -f PrivShield console-backend-go

# 停止并移除容器与内部网络
docker compose down
```

### 5.3 Profile 机制与 LLM 解耦

Agent 与 vLLM 采用松耦合设计：
- vLLM 独立容器通过 OpenAI 兼容 HTTP 接口（`:8000/v1`）提供分类平滑能力；
- 若 vLLM 重启或未启动，Java Agent 自动平滑降级到 Layer-1 规则引擎与 Layer-2 NER 引擎，业务请求不受阻塞。

---

## 6. Kubernetes 原生部署 (`deploy/k8s/`)

`deploy/k8s/` 提供了生产级 K8s 资源清单与 Kustomize 配置：

```bash
cd deploy/k8s

# 1. 创建命名空间
kubectl apply -f namespace.yaml

# 2. 配置 ConfigMap 与 Secret
kubectl apply -f configmap.yaml
kubectl apply -f secret.example.yaml

# 3. 部署 Java Agent 与 Service
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml

# 4. 可选：部署独立 LLM 服务
kubectl apply -f llm-deployment.yaml
kubectl apply -f llm-service.yaml

# 5. 或使用 Kustomize 一键部署
kubectl apply -k .

# 6. 验证 Pod 状态
kubectl get pods -n PrivShield -w
```

---

## 7. Helm 生产发布 (`deploy/helm/`)

`deploy/helm/PrivShield` 提供了高度可配置的生产级 Helm Chart：

```bash
cd deploy/helm

# 1. 静态语法检查与模板渲染测试
helm lint PrivShield
helm template test-release PrivShield --values PrivShield/values-production.yaml

# 2. 开发环境部署
helm install privshield-dev PrivShield/ \
  --namespace privshield --create-namespace

# 3. 生产环境部署（启用双副本、HPA 自动扩缩、资源配额限制）
helm install privshield-prod PrivShield/ \
  --namespace privshield --create-namespace \
  -f PrivShield/values-production.yaml

# 4. 滚动升级
helm upgrade privshield-prod PrivShield/ \
  --namespace privshield \
  -f PrivShield/values-production.yaml

# 5. 查看版本发布历史与秒级回滚
helm history privshield-prod -n privshield
helm rollback privshield-prod 1 -n privshield

# 6. 卸载 Release
helm uninstall privshield-prod -n privshield
```

---

## 8. 安全配置 (mTLS / API Key / 限流)

### 8.1 gRPC mTLS 双向证书认证

1. **生成自签名证书链**：
   ```bash
   bash console/backend-go/scripts/gen-certs.sh console/backend-go/certs
   ```
2. **Agent 启用 TLS**：
   设置环境变量 `PRIVACY_TLS_ENABLED=true`，配置服务端证书与 CA 证书。
3. **Go 客户端启用 TLS**：
   设置 `PRIVACY_AGENT_TLS_ENABLED=true`，挂载 `client.crt`、`client.key` 与 `ca.crt`。

### 8.2 API Key 鉴权

在 `application.yml` 或环境变量中启用：
```yaml
security:
  auth-enabled: true
  api-key: "sk-privshield-secret-key"
```
客户端请求时在 HTTP Header 中携带 `X-API-Key: sk-privshield-secret-key`。

### 8.3 速率限制 (Rate Limiting)

```yaml
security:
  rate-limit-enabled: true
  rate-limit-rps: 100
```

---

## 9. 健康检查与探针体系

| 检查维度 | 端点 | 协议 | 适用场景 | 预期响应 |
|---|---|---|---|---|
| **Liveness 存活探针** | `/health` | HTTP | K8s LivenessProbe / Docker Healthcheck | `{"status":"ok","namespace":"default"}` (HTTP 200) |
| **Actuator 监控** | `/actuator/health` | HTTP | Spring Boot Actuator 健康监控 | `{"status":"UP"}` (HTTP 200) |
| **Prometheus 指标** | `/actuator/prometheus` | HTTP | Prometheus 定期 Scrape 抓取 | 文本时序指标 (HTTP 200) |
| **gRPC 存活** | `grpc.health.v1.Health` | gRPC | gRPC 客户端健康探针 | `SERVING` |
| **Go 网关健康** | `/api/health` | HTTP | Console UI / 负载均衡探针 | `{"backend":"ok","agent":{"status":"ok"}}` |

---

## 10. 监控告警与可观测性 (Prometheus + Grafana)

1. **Prometheus 配置** (`deploy/prometheus/prometheus.yml`)：
   配置抓取 Java Agent 的指标端点（默认间隔 `15s`）。
2. **预置告警规则** (`deploy/prometheus/alerts.yml`)：
   - `GatewayNoHealthyNodes`：可用后端节点为 0（严重告警）。
   - `HighRequestLatencyP95`：P95 请求延迟 > 1s 持续 5 分钟。
   - `HighGatewayErrorRate`：5xx 错误率 > 5% 持续 5 分钟。
   - `PrivacyBudgetNearlyExhausted`：差分隐私预算余量 < 0.1。
3. **Grafana 仪表盘** (`deploy/grafana/dashboard.json`)：
   预置了请求 QPS、P50/P95/P99 延迟分位数、脱敏算法吞吐、DP 预算消耗曲线图表。

---

## 11. 常见故障排查 (Troubleshooting)

### Q1: 端口冲突（8079 / 50051 / 8081 / 5173 被占用）
- **现象**：启动脚本报错 `端口已被占用`。
- **排查与解决**：
  - 运行 `./console/scripts/dev-stop.sh` 或 `./console/scripts/prod-stop.sh` 自动清理残留进程。
  - 使用 `./console/scripts/dev-start-go.sh --force` 自动强制终止冲突进程。
  - 手动定位：`lsof -i :8079` 或 `ss -tlnp | grep 8079`。

### Q2: Java Agent JAR 包未找到
- **现象**：`未找到 Java Agent jar`。
- **解决**：脚本会自动触发 Maven 编译；亦可手动进入 `agent/` 目录执行 `mvn clean package -DskipTests` 生成 `agent-server/target/agent-server-0.1.0-SNAPSHOT.jar`。

### Q3: 前端控制台提示 `Network Error` 或无法连接 Agent
- **排查**：
  1. 访问 `http://127.0.0.1:8079/health` 检查 Java Agent 是否正常运行。
  2. 访问 `http://127.0.0.1:8081/api/health` 检查 Go 代理后端与 Agent 之间的 gRPC 连通性。
  3. 检查控制台页面顶部 **Backend Selector** 是否选择了正确的后端端口。

