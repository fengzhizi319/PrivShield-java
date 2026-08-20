# PrivShield Java — 企业级数据隐私计算 Java Agent 与控制台

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Go](https://img.shields.io/badge/Go-1.21%2B%20%7C%201.24%2B-blue.svg)](https://golang.org/)
[![React](https://img.shields.io/badge/React-18.3-61dafb.svg)](https://reactjs.org/)
[![gRPC](https://img.shields.io/badge/gRPC-1.62.2-blueviolet.svg)](https://grpc.io/)
[![Docker](https://img.shields.io/badge/Docker-20.10%2B-2496ed.svg)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-1.24%2B%20%7C%201.28%2B-326ce5.svg)](https://kubernetes.io/)
[![Helm](https://img.shields.io/badge/Helm-3.10%2B%20%7C%203.12%2B-0f1689.svg)](https://helm.sh/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Tests](https://img.shields.io/badge/Tests-88%2B%20Passed-success.svg)](scripts/dev/run_ops_tests.sh)

> **PrivShield Java** —— 企业级数据隐私计算与动态分类治理边车（Sidecar）服务，提供 **REST (Spring Boot 3)** 与 **gRPC (Netty)** 双协议高性能并发架构。
>
> 完整提供数据智能脱敏、差分隐私加噪与预算持久化、K-匿名化准标识符泛化、查询混淆、医疗/金融合规流水线以及三层动态分类分级漏斗引擎（L1 规则 ➔ L2 NER ➔ L3 LLM GPU 平滑）。配套提供基于 Go gRPC 高性能代理与 React 18 单页应用的综合控制台，支持跨平台一键自动化运维、Docker Compose 编排、Kubernetes 原生与 Helm 生产发布体系。
>
> 🌐 **GitHub 仓库**: [https://github.com/fengzhizi319/PrivShield-java](https://github.com/fengzhizi319/PrivShield-java)

---

## 目录

- [1. 架构概览与拓扑设计](#1-架构概览与拓扑设计)
  - [1.1 系统全景拓扑](#11-系统全景拓扑)
  - [1.2 项目模块结构](#12-项目模块结构)
  - [1.3 核心技术栈](#13-核心技术栈)
- [2. 快速开始](#2-快速开始)
  - [2.1 前置环境准备](#21-前置环境准备)
  - [2.2 一键式极速启动 (推荐)](#22-一键式极速启动-推荐)
  - [2.3 分组件手动编译与启动](#23-分组件手动编译与启动)
- [3. 控制台自动化运维脚本体系 (`console/scripts/`)](#3-控制台自动化运维脚本体系-consolescripts)
  - [3.1 开发模式脚本 (`dev-*.sh`)](#31-开发模式脚本-dev-sh)
  - [3.2 生产静态托管模式脚本 (`prod-*.sh`)](#32-生产静态托管模式脚本-prod-sh)
  - [3.3 Docker 容器化编排脚本 (`docker-*.sh`)](#33-docker-容器化编排脚本-docker-sh)
  - [3.4 脚本参数矩阵与非交互式运维](#34-脚本参数矩阵与非交互式运维)
- [4. 云原生生产部署与运维全景 (`deploy/`)](#4-云原生生产部署与运维全景-deploy)
  - [4.1 多阶段容器镜像构建架构](#41-多阶段容器镜像构建架构)
  - [4.2 Docker Compose 全栈多环境编排 (`deploy/docker-compose/`)](#42-docker-compose-全栈多环境编排-deploydocker-compose)
  - [4.3 Kubernetes 原生与 Kustomize 生产发布 (`deploy/k8s/`)](#43-kubernetes-原生与-kustomize-生产发布-deployk8s)
  - [4.4 Helm 生产发布与弹性伸缩 (`deploy/helm/`)](#44-helm-生产发布与弹性伸缩-deployhelm)
  - [4.5 全链路安全加固 (mTLS / API Key / 限流)](#45-全链路安全加固-mtls--api-key--限流)
  - [4.6 监控告警与可观测性体系 (Prometheus + Grafana)](#46-监控告警与可观测性体系-prometheus--grafana)
  - [4.7 生产日常运维、备份与故障排查指南 (Day-2 Ops & FAQ)](#47-生产日常运维备份与故障排查指南-day-2-ops--faq)
- [5. 运维与部署全套自动化测试 (`tests/`)](#5-运维与部署全套自动化测试-tests)
  - [5.1 自动化测试分层架构](#51-自动化测试分层架构)
  - [5.2 一键执行自动化测试](#52-一键执行自动化测试)
  - [5.3 CI/CD 质量门禁配置](#53-cicd-质量门禁配置)
- [6. 核心隐私计算功能与实践](#6-核心隐私计算功能与实践)
  - [6.1 智能数据脱敏 (Data Masking)](#61-智能数据脱敏-data-masking)
  - [6.2 差分隐私计算 (Differential Privacy)](#62-差分隐私计算-differential-privacy)
  - [6.3 K-匿名化多维泛化 (K-Anonymity)](#63-k-匿名化多维泛化-k-anonymity)
  - [6.4 查询混淆与假名注入 (Query Obfuscation)](#64-查询混淆与假名注入-query-obfuscation)
  - [6.5 医疗流水线一体化治理 (Medical Pipeline)](#65-医疗流水线一体化治理-medical-pipeline)
  - [6.6 三层动态分类分级漏斗 (DynClassification)](#66-三层动态分类分级漏斗-dynclassification)
- [7. API 参考手册](#7-api-参考手册)
  - [7.1 gRPC RPC 服务端点 (33 个 RPC)](#71-grpc-rpc-服务端点-33-个-rpc)
  - [7.2 REST HTTP 控制器端点 (13 个 Controller)](#72-rest-http-控制器端点-13-个-controller)
- [8. 规则与分类体系配置](#8-规则与分类体系配置)
- [9. 项目文档索引与导航](#9-项目文档索引与导航)
- [10. 许可证与相关项目](#10-许可证与相关项目)

---

## 1. 架构概览与拓扑设计

### 1.1 系统全景拓扑

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   PrivShield Java 生产架构全景                                  │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                                 │
│    [ 客户端 / 浏览器 / CI 管道 ]                                                                │
│                 │                                                                               │
│                 ▼ (HTTP / Nginx Ingress :80/:443)                                               │
│    ┌───────────────────────────┐                ┌──────────────────────────────────────────┐    │
│    │     Console Web UI        │                │         Console Backend (Go)             │    │
│    │  (React 18 + Vite + i18n) │───────────────▶│    (Go 1.23+ Gin gRPC 代理网关)          │    │
│    │   Dev: :5173 / Prod: 静态 │                │     监听端口: :8081  (支持 mTLS)          │    │
│    └─────────────┬─────────────┘                └────────────────────┬─────────────────────┘    │
│                  │                                                   │ (高性能 gRPC :50051)      │
│                  │ (备选 REST:8080)                                  │                          │
│                  ▼                                                   │                          │
│    ┌───────────────────────────┐                                     │                          │
│    │   Console Backend (Py)    │                                     │                          │
│    │  (FastAPI REST 兼容代理)  │─────────────┐                       │                          │
│    │     监听端口: :8080       │             │ (REST :8079)          │                          │
│    └───────────────────────────┘             ▼                       ▼                          │
│                                 ┌──────────────────────────────────────────────────────────┐    │
│                                 │                  PrivShield Java Agent                   │    │
│                                 │       (Spring Boot 3.3.5 / JDK 17+ / Netty gRPC)         │    │
│                                 │     REST 接口: :8079  |  gRPC 接口: :50051 (含 TLS)      │    │
│                                 │     Actuator: /actuator/health /actuator/prometheus      │    │
│                                 └────────────────────────────┬─────────────────────────────┘    │
│                                                              │                                  │
│                 ┌────────────────────────────────────────────┼──────────────────────────────┐   │
│                 ▼                                            ▼                              ▼   │
│   ┌───────────────────────────┐                ┌───────────────────────────┐  ┌──────────────┐  │
│   │    Privacy Core (SDK)     │                │   DynClassification       │  │ 数据持久化   │  │
│   │  - 数据脱敏 (13+ 规则引擎)│                │   - Layer 1: 正则/字典规则│  │ - 预算 DB    │  │
│   │  - 差分隐私 (Laplace/Gauss│                │   - Layer 2: ONNX NER 实体│  │   (SQLite)   │  │
│   │  - K-匿名化 (Mondrian)    │                │   - Layer 3: vLLM GPU 推理│  │ - 审计日志   │  │
│   │  - 查询混淆 / 医疗流水线  │                └─────────────┬─────────────┘  │   (/var/log) │  │
│   └───────────────────────────┘                              │                └──────────────┘  │
│                                                              │ (HTTP 短连接 :8000/v1)           │
│                                                              ▼                                  │
│                                                ┌───────────────────────────┐                    │
│                                                │   vLLM GPU 推理容器(可选) │                    │
│                                                │  Qwen3.5-0.8B-Classifier  │                    │
│                                                └───────────────────────────┘                    │
│                                                                                                 │
│    [ 可观测性监控栈 ]                                                                           │
│    ┌───────────────────────────┐                ┌───────────────────────────┐                   │
│    │   Prometheus (:9090)      │───────────────▶│     Grafana (:3000)       │                   │
│    │   时序指标定期 Scrape 采集 │                │  多维度 QPS/P99 延迟/DP看板│                   │
│    └───────────────────────────┘                └───────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 项目模块结构

```
PrivShield-java/
├── agent/                         # Java Agent 核心主模块
│   ├── agent-sdk/                 # 隐私算法 SDK (脱敏/DP/KAnon/QoL/三层分类引擎)
│   ├── agent-server/              # Spring Boot 服务端 (33 个 gRPC + 13 个 REST Controllers)
│   ├── config/                    # 服务端配置文件 (application.yml 等)
│   ├── rules/                     # 分类分级与脱敏规则 YAML 库 (domains/standards/taxonomies)
│   ├── scripts/                   # Agent 专属启停脚本
│   └── Dockerfile                 # Java Agent 多阶段容器构建定义
├── console/                       # 控制台微服务模块
│   ├── backend-go/                # Go gRPC 代理网关 (Gin 框架 / gRPC 客户端 / 静态托管 / Dockerfile)
│   ├── backend/                   # Python FastAPI 代理网关 (备选 REST 通道)
│   ├── web/                       # React 18 + Vite + TypeScript 前端 SPA
│   └── scripts/                   # 控制台一键式跨平台运维脚本集
├── deploy/                        # 云原生生产部署与运维资产库
│   ├── docker-compose/            # Docker Compose 全栈编排 (dev / prod / test)
│   ├── k8s/                       # Kubernetes 原生清单 (Deployment/Service/ConfigMap/Kustomize)
│   ├── helm/                      # 生产级 Helm Chart (HPA/NetworkPolicy/ServiceMonitor/PDB)
│   ├── prometheus/                # Prometheus 抓取配置与告警规则 (alerts.yml)
│   └── grafana/                   # Grafana 监控可视化仪表盘 (dashboard.json)
├── scripts/                       # 项目级运维与自动化测试脚本
│   ├── dev/                       # 开发阶段脚本 (run_ops_tests.sh / health_check.sh 等)
│   └── prod/                      # 生产发布脚本 (deploy-helm.sh / backup_privacy_budget.sh 等)
├── tests/                         # 运维与部署全套自动化测试套件
│   ├── deployment/                # Helm 语法/模板渲染与 K8s/Compose 验证测试
│   └── ops/                       # Shell 脚本规范、探针响应契约与健康指标测试
└── privacy-java-sdk/              # 独立可发布的 Java 隐私计算基础库
```

### 1.3 核心技术栈

| 层次 | 核心技术选型 | 说明 |
|---|---|---|
| **Agent 核心层** | Java 17, Spring Boot 3.3.5, Netty gRPC 1.62.2, Jackson | 高吞吐低延迟，支持虚拟线程与 JRE 瘦身 |
| **代理网关层** | Go 1.23+, Gin 1.10, gRPC-Go 1.62, Zap Logger | 高并发 gRPC ➔ REST 网关，支持直接托管前端产物 |
| **前端展现层** | React 18, Vite 6, TypeScript 5, TailwindCSS, Lucide Icons | 毫秒级 HMR 热更新，响应式布局与多语言支持 |
| **容器与集群编排** | Docker Engine 24+, Docker Compose v2, Kubernetes 1.28+, Helm 3.12+ | 支持容器多阶段构建、HPA 弹性扩缩容 |
| **可观测性体系** | Spring Boot Actuator, Micrometer, Prometheus, Grafana | 统一时序指标采集与可视化告警 |

---

## 2. 快速开始

### 2.1 前置环境准备

- **JDK**: 17+ (推荐 Eclipse Temurin 17 / OpenJDK 17)
- **Maven**: 3.8.0+
- **Go**: 1.21+ (开发 Go 后端必需)
- **Node.js**: 16.14+ (推荐 Node 18+ 或 20+，配套 pnpm / npm)
- **Docker**: 20.10+ (容器化模式必需)

### 2.2 一键式极速启动 (推荐)

项目提供了自动检测环境、自动解析/编译缺失产物、自动处理端口冲突的智能脚本：

```bash
# 🚀 启动推荐开发环境 (Java Agent + Go 后端 + 前端 Vite 毫秒级 HMR)
./console/scripts/dev-start-go.sh

# 🌐 启动生产预览环境 (编译打包前端并在 :8081 由 Go 后端统一托管 UI 与 API)
./console/scripts/prod-start-go.sh

# 🪟 Windows PowerShell 开发启动
powershell -ExecutionPolicy Bypass -File console\scripts\dev-start-go.ps1
```

启动完成后，直接在浏览器访问：
- **开发前端 (Vite HMR)**: `http://localhost:5173`
- **生产控制台 UI & API**: `http://127.0.0.1:8081`
- **Agent REST 接口**: `http://127.0.0.1:8079`
- **Agent gRPC 接口**: `127.0.0.1:50051`

### 2.3 分组件手动编译与启动

#### 步骤 1：编译并启动 Java Agent
```bash
cd agent
mvn clean package -DskipTests
java -jar agent-server/target/agent-server-0.1.0-SNAPSHOT.jar \
  --server.port=8079 --grpc.server.port=50051
```

#### 步骤 2：编译并启动 Go 代理后端
```bash
cd console/backend-go
go build -o server ./cmd/server
./server
```

#### 步骤 3：启动前端 Web 控制台
```bash
cd console/web
pnpm install
pnpm dev
```

---

## 3. 控制台自动化运维脚本体系 (`console/scripts/`)

`console/scripts/` 目录为开发、生产部署与 Docker 容器环境提供了标准化的跨平台生命周期管理脚本。

### 3.1 开发模式脚本 (`dev-*.sh`)

开发模式启动前端 Vite 开发服务器（`:5173`），具备 `<50ms` 的模块热替换能力：

| 脚本命令 | 启动组件清单 | 核心特性与适用场景 |
|---|---|---|
| [`./console/scripts/dev-start-go.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/dev-start-go.sh) | Java Agent + Go 代理 (`:8081`) + Vite (`:5173`) | **主力开发首选**。基于 gRPC 链路通信，性能最佳 |
| [`./console/scripts/dev-start-all.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/dev-start-all.sh) | Java Agent + Python (`:8080`) + Go (`:8081`) + Vite (`:5173`) | **双后端模式**。支持在前端右上角随时无缝切换后端通道 |
| [`./console/scripts/dev-start.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/dev-start.sh) | Java Agent + Python REST 代理 (`:8080`) + Vite (`:5173`) | Python REST 代理开发模式 |
| [`./console/scripts/dev-start-go-mtls.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/dev-start-go-mtls.sh) | Java Agent + Go 代理 (mTLS) + Vite (`:5173`) | **mTLS 加密模式**。自动生成 CA 与双向证书链并启用 TLS 认证 |
| [`./console/scripts/dev-stop.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/dev-stop.sh) | - | **一键停止**。安全终止所有开发模式进程并彻底释放端口 |

### 3.2 生产静态托管模式脚本 (`prod-*.sh`)

生产模式会自动将前端打包编译为优化静态资源（`console/web/dist`），由 Go 或 Python 后端直接提供静态托管与路由回退，**无需运行 Node.js 进程**：

| 脚本命令 | 对外访问入口 | 说明 |
|---|---|---|
| [`./console/scripts/prod-start-go.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/prod-start-go.sh) | `http://127.0.0.1:8081` | Go 代理后端直接托管静态 UI 与 API，单端口搞定 |
| [`./console/scripts/prod-start-all.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/prod-start-all.sh) | Python `:8080` / Go `:8081` | 双后端并发静态托管，用于对比压测与冗余容灾 |
| [`./console/scripts/prod-start.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/prod-start.sh) | `http://127.0.0.1:8080` | Python REST 代理静态托管模式 |
| [`./console/scripts/prod-start-go-mtls.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/prod-start-go-mtls.sh) | `http://127.0.0.1:8081` | mTLS 双向认证生产预览环境 |
| [`./console/scripts/prod-stop.sh`](file:///home/charles/code/sfwork/PrivShield-java/console/scripts/prod-stop.sh) | - | **一键停止**。安全终止所有生产模式后台进程并释放端口 |

### 3.3 Docker 容器化编排脚本 (`docker-*.sh`)

基于 Docker Compose 快速编排隔离的容器套件：

```bash
# 启动 Java Agent + Go 代理 + React Web UI 容器套件
./console/scripts/docker-start-go.sh

# 启动 Java Agent + Python 代理 + React Web UI 容器套件
./console/scripts/docker-start-python.sh

# 启动全栈套件 (Agent + 双后端 + Web UI)
./console/scripts/docker-start-all.sh

# 启动全栈套件 + vLLM 大模型 GPU 推理容器
./console/scripts/docker-start-all.sh --with-llm

# 优雅停止并清理全部容器与 Docker 网络
./console/scripts/docker-stop.sh
```

### 3.4 脚本参数矩阵与非交互式运维

所有启动脚本均已实现标准化参数支持：

- `-f, --force`: 遇到端口占用时自动终止旧进程，适用于 CI 脚本或无交互自动化部署；
- `--rebuild`: 强制清除既有编译缓存并重新触发 Maven / Go / Vite 构建打包；
- `-h, --help`: 查看详细的使用参数与示例说明。

---

## 4. 云原生生产部署与运维全景 (`deploy/`)

### 4.1 多阶段容器镜像构建架构

所有微服务均遵循严格的多阶段瘦身与非 Root 安全标准：

```bash
# 1. 构建 Java Agent 生产镜像 (Maven 编译 ➔ Temurin-17-JRE Alpine 瘦身运行时)
cd agent
docker build -t privshield-java-agent:0.1.0 .

# 2. 构建 Go 代理网关镜像 (Golang 静态编译 ➔ Alpine 极简运行时，仅 ~15MB)
cd console/backend-go
docker build -t privacy-console-backend-go:0.1.0 .

# 3. 构建 Web 前端 Nginx 托管镜像 (Node 打包 ➔ Nginx Alpine 静态代理)
cd console/web
docker build -t privacy-console-web:0.1.0 .
```

### 4.2 Docker Compose 全栈多环境编排 (`deploy/docker-compose/`)

编排定义位于 [`deploy/docker-compose/`](file:///home/charles/code/sfwork/PrivShield-java/deploy/docker-compose/)：

```bash
cd deploy/docker-compose

# 1. 基础启动：Java Agent + Go 代理 + Python 代理 + Web UI
docker compose up -d

# 2. 启用 GPU 独立推理容器 (vLLM OpenAI 兼容接口，core 自动平滑解耦)
docker compose --profile llm up -d

# 3. 启用 Prometheus (:9090) 与 Grafana (:3000) 监控看板
docker compose --profile monitoring up -d

# 4. 一键拉起全栈（基础 + LLM + 监控栈）
docker compose --profile llm --profile monitoring up -d

# 5. 生产加固模式 (启用 Redis 限流与 JSON 结构化日志)
docker compose -f docker-compose.prod.yml up -d

# 6. 查看服务状态与实时日志
docker compose ps
docker compose logs -f PrivShield console-backend-go

# 7. 优雅下线并清理网络与卷
docker compose down
```

### 4.3 Kubernetes 原生与 Kustomize 生产发布 (`deploy/k8s/`)

资源清单位于 [`deploy/k8s/`](file:///home/charles/code/sfwork/PrivShield-java/deploy/k8s/)：

```bash
cd deploy/k8s

# 1. 使用 Kustomize 一键交付全套命名空间、配置与服务
kubectl apply -k .

# 2. 观察滚动发布状态
kubectl rollout status deployment/PrivShield -n PrivShield

# 3. 检查 Pod 与 Service 端点
kubectl get pods,svc,endpoints -n PrivShield -o wide
```

### 4.4 Helm 生产发布与弹性伸缩 (`deploy/helm/`)

生产级 Helm Chart 位于 [`deploy/helm/PrivShield`](file:///home/charles/code/sfwork/PrivShield-java/deploy/helm/PrivShield)：

| 配置维度 | 开发默认 (`values.yaml`) | 生产规范 (`values-production.yaml`) | 生产设计原因 |
|---|---|---|---|
| **副本数与伸缩** | 1 副本，无 HPA | 2 副本起步，HPA 自动扩缩 (2~10 副本，CPU 70%/内存 80%) | 保证多副本高可用，支撑突发流量 |
| **日志格式** | `text` (纯文本) | `json` (结构化 JSON) | 便于 ELK / Loki / 云日志统一收集与字段解析 |
| **TLS 传输加密** | 关闭 | 强制开启，通过 K8s Secret 挂载证书 | 防止网段抓包与中间人窃听 |
| **认证与限流** | 关闭 | 强制 API Key 鉴权与令牌桶限流 | 保护计算资源，防止恶意未授权调用与 DoS 攻击 |
| **网络隔离** | 开放 | 启用 NetworkPolicy 零信任隔离 | 仅允许集群内授权 Pod 访问隐私端口 |
| **可观测性** | 关闭 | 启用 ServiceMonitor (HTTPS) | 自动对接 Prometheus Operator |

```bash
cd deploy/helm

# 1. 静态语法校验
helm lint PrivShield -f PrivShield/values-production.yaml

# 2. 生产环境发布
helm install privshield-prod PrivShield/ \
  --namespace privshield --create-namespace \
  -f PrivShield/values-production.yaml

# 3. 生产平滑滚动升级
helm upgrade privshield-prod PrivShield/ -f PrivShield/values-production.yaml

# 4. 查看发布历史与秒级快速回滚
helm history privshield-prod -n privshield
helm rollback privshield-prod 1 -n privshield
```

### 4.5 全链路安全加固 (mTLS / API Key / 限流)

1. **gRPC mTLS 双向认证**：
   - 自动生成根 CA、服务端与客户端双向证书链：
     ```bash
     bash console/backend-go/scripts/gen-certs.sh console/backend-go/certs
     ```
   - 在 Agent 端设置 `PRIVACY_TLS_ENABLED=true`，并注入证书路径；在 Go 代理端启用客户端证书配置。
2. **API Key 认证**：
   在 `application.yml` 或环境变量中启用 `security.auth-enabled: true`，并在 HTTP Header 中附带 `X-API-Key: sk-privshield-secret-key`。
3. **高并发限流防护**：
   配置 `security.rate-limit-enabled: true`，支持针对 IP 与 API Key 维度的令牌桶限流。

### 4.6 监控告警与可观测性体系 (Prometheus + Grafana)

1. **Prometheus 抓取配置** ([`deploy/prometheus/prometheus.yml`](file:///home/charles/code/sfwork/PrivShield-java/deploy/prometheus/prometheus.yml))：定期抓取 Java Agent 的 `/actuator/prometheus` 端点；
2. **企业级告警规则** ([`deploy/prometheus/alerts.yml`](file:///home/charles/code/sfwork/PrivShield-java/deploy/prometheus/alerts.yml))：
   - `GatewayNoHealthyNodes`: 网关健康后端节点为 0 (Critical)；
   - `HighRequestLatencyP95`: P95 接口延迟 > 1s 持续 5 分钟 (Warning)；
   - `HighGatewayErrorRate`: 网关 5xx 错误率 > 5% (Critical)；
   - `PrivacyBudgetNearlyExhausted`: 差分隐私预算余量 < 0.1 (Warning)；
3. **Grafana 监控看板** ([`deploy/grafana/dashboard.json`](file:///home/charles/code/sfwork/PrivShield-java/deploy/grafana/dashboard.json))：开箱即用的实时 QPS、分位数延迟、脱敏算法处理量与差分隐私预算曲线。

### 4.7 生产日常运维、备份与故障排查指南 (Day-2 Ops & FAQ)

#### 1. 端口冲突排查与两段式清理
- 若启动报端口占用，可执行 `./console/scripts/dev-stop.sh` 或 `./console/scripts/prod-stop.sh`；脚本先发 `SIGTERM` 允许平滑释放资源，1秒后若未退出则自动升级为 `SIGKILL`。

#### 2. 隐私预算持久化与日常备份维护
- 隐私预算支持跨实例 SQLite 持久化：
  ```bash
  # 备份隐私预算数据库
  ./scripts/prod/backup_privacy_budget.sh /backup/path
  
  # 重置/清理预算数据库
  ./scripts/dev/clean_privacy_budget_db.sh
  ```

#### 3. 健康检查与探针矩阵速查

| 探测端点 | 协议 | 用途与适用场景 | 预期成功响应 |
|---|---|---|---|
| `GET /health` | HTTP | 容器 Liveness 存活探针 | `{"status":"ok","namespace":"default"}` (HTTP 200) |
| `GET /actuator/health` | HTTP | Spring Boot Actuator 内部组件健康 | `{"status":"UP"}` (HTTP 200) |
| `GET /actuator/prometheus` | HTTP | Prometheus 指标采集抓取 | 文本时序指标 (HTTP 200) |
| `GET /api/health` | HTTP | Go 代理后端网关探测 | `{"backend":"ok","agent":{"status":"ok"},"protocol":"gRPC"}` |
| `grpc.health.v1.Health` | gRPC | gRPC 客户端健康探针 | `SERVING` |

---

## 5. 运维与部署全套自动化测试 (`tests/`)

### 5.1 自动化测试分层架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      部署与运维自动化测试分层体系                       │
├─────────────────────────────────────────────────────────────────────────┤
│  1. 静态合规层: bash -n 语法、Shebang 规范、Helm Lint、PyYAML 文档校验  │
│  2. 模板渲染层: Helm Template 断言 (HPA, NetworkPolicy, PDB, 探针)      │
│  3. 运行时契约层: Go 单元测试、Prometheus 文本协议校验、JSON Schema 断言│
│  4. E2E 冒烟层: 一键全生命周期起停、HTTP ➔ gRPC ➔ Agent 全链路穿透验证   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 一键执行自动化测试

项目内置了一键式全套运维与部署测试脚本 [`scripts/dev/run_ops_tests.sh`](file:///home/charles/code/sfwork/PrivShield-java/scripts/dev/run_ops_tests.sh)：

```bash
# 运行全部 88+ 项静态检查、Helm 校验、K8s 验证与 Go 单元测试
./scripts/dev/run_ops_tests.sh

# 运行全套测试 + 自动拉起服务执行端到端 (E2E) 冒烟测试
./scripts/dev/run_ops_tests.sh --with-e2e
```

**分模块独立测试命令**：
```bash
# 运行 Pytest 部署与运维测试套件
pytest tests/ -v

# 运行 Go 代理后端单元与路由测试
cd console/backend-go && go test ./... -v

# 运行 Helm Chart 静态合规检查
helm lint deploy/helm/PrivShield -f deploy/helm/PrivShield/values-production.yaml
```

### 5.3 CI/CD 质量门禁配置

可将测试脚本直接接入 GitHub Actions 或 GitLab CI：

```yaml
# .github/workflows/ops-test.yml
name: Ops & Deployment Validation
on: [push, pull_request]
jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '17', distribution: 'temurin' }
      - uses: actions/setup-go@v5
        with: { go-version: '1.23' }
      - uses: azure/setup-helm@v4
      - uses: actions/setup-python@v5
        with: { python-version: '3.11' }
      - run: pip install pytest pyyaml
      - run: ./scripts/dev/run_ops_tests.sh
```

---

## 6. 核心隐私计算功能与实践

### 6.1 智能数据脱敏 (Data Masking)

支持手机号、身份证、姓名、银行卡、邮箱、地址等 13+ 类敏感字段掩码：

```bash
curl -X POST http://localhost:8079/v1/privacy/mask \
  -H "Content-Type: application/json" \
  -d '{"value": "张三 13812345678 110101199001011234", "field_name": "mixed"}'

# 响应: {"result": "张**8"}
```

### 6.2 差分隐私计算 (Differential Privacy)

支持 Laplace 与 Gaussian 机制下的 Count/Sum/Mean 加噪与隐私预算实时追踪扣减：

```bash
curl -X POST http://localhost:8079/v1/privacy/dp/count \
  -H "Content-Type: application/json" \
  -d '{"values": [1,2,3,4,5], "epsilon": 1.0}'

# 响应: {"result": 4.5} (真实值=5, 注入可控 Laplace 噪声)
```

### 6.3 K-匿名化多维泛化 (K-Anonymity)

基于 Mondrian 多维贪心分割算法实现准标识符 (QI) 泛化：

```bash
curl -X POST http://localhost:8079/v1/privacy/k_anonymize/record \
  -H "Content-Type: application/json" \
  -d '{
    "record": {"name": "张三", "age": "30", "city": "北京"},
    "qi_cols": ["name", "age"],
    "k": 2
  }'
```

### 6.4 查询混淆与假名注入 (Query Obfuscation)

SQL 查询混淆 + 噪声假名注入保护：

```bash
curl -X POST http://localhost:8079/v1/privacy/qol/obfuscate \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT * FROM patients WHERE name=\"张三\"",
    "columns": ["name"]
  }'
```

### 6.5 医疗流水线一体化治理 (Medical Pipeline)

针对病历、处方、检验报告的一体化合规分类与脱敏流水线：

```bash
curl -X POST http://localhost:8079/v1/medical_pipeline/process_records \
  -H "Content-Type: application/json" \
  -d '{
    "records": [{"name": "李四", "phone": "13900001111", "diagnosis": "高血压"}]
  }'
```

### 6.6 三层动态分类分级漏斗 (DynClassification)

采用 **Layer 1 规则过滤 ➔ Layer 2 NER 实体识别 ➔ Layer 3 LLM 语义平滑** 的漏斗结构：

```bash
curl -X POST http://localhost:8079/v1/dynclassification/eval \
  -H "Content-Type: application/json" \
  -d '{"field_name": "name", "value": "王五", "domain": "medical"}'
```

---

## 7. API 参考手册

### 7.1 gRPC RPC 服务端点 (33 个 RPC)

| 分组 | 方法名 | 说明 |
|---|---|---|
| **脱敏与匿名化** | `Mask`, `MaskBatch`, `MaskFile` | 数据掩码与批量文件脱敏 |
| **差分隐私** | `DPCount`, `DPSum`, `DPMean`, `DPBudget` | 差分隐私加噪计算与预算查询 |
| **K-匿名化** | `KAnonymizeRecord`, `KAnonymizeTable` | 准标识符泛化处理 |
| **查询混淆** | `QolObfuscate` | SQL 假名与噪声混淆 |
| **动态分类分级** | `ClassifyField`, `ClassifyRecord`, `ClassifyTable` | 三层动态分类分级评级 |
| **流水线与健康** | `ProcessMedicalRecords`, `Health`, `GetStats` | 一体化处理与健康状态探测 |

### 7.2 REST HTTP 控制器端点 (13 个 Controller)

| HTTP 方法 | 路径 | 功能说明 |
|---|---|---|
| `POST` | `/v1/privacy/mask` | 数据字段智能脱敏 |
| `POST` | `/v1/privacy/dp/count` | 差分隐私计数加噪 |
| `POST` | `/v1/privacy/dp/sum` | 差分隐私求和加噪 |
| `POST` | `/v1/privacy/dp/mean` | 差分隐私均值加噪 |
| `POST` | `/v1/privacy/k_anonymize/record` | 记录级 K-匿名化泛化 |
| `POST` | `/v1/privacy/qol/obfuscate` | SQL 查询混淆 |
| `POST` | `/v1/dynclassification/eval` | 字段动态分类分级 |
| `POST` | `/v1/dynclassification/eval_record` | 记录级动态分类分级 |
| `POST` | `/v1/medical_pipeline/process_records`| 医疗健康一体化流水线处理 |
| `GET` | `/health` | 服务存活探测 (Liveness Probe) |
| `GET` | `/actuator/health` | Spring Boot Actuator 组件健康状态 |
| `GET` | `/actuator/prometheus` | Prometheus 时序指标抓取端点 |
| `GET` | `/api/health` (Go Proxy) | 控制台网关健康与 Agent 连通性探测 |

---

## 8. 规则与分类体系配置

规则文件统一存放于 `agent/rules/` 目录下：

```
agent/rules/
├── domains/          # 领域规则定义 (medical.yaml, finance.yaml, general-pii.yaml)
├── standards/        # 行业合规标准 (gd_health.yaml 广电医疗, jrt0197.yaml 金融数据安全)
└── taxonomies/       # 分类分级等级体系 (default.yaml, finance_jrt0197.yaml)
```

---

## 9. 项目文档索引与导航

- 📘 [部署与运维全手册](docs/deployment/ops.md)
- 🧪 [部署与运维测试指南](docs/deployment/testing.md)
- 🕹️ [Console 自动化运行脚本手册](console/scripts/README.md)
- 🐳 [Docker Compose 部署指南](deploy/docker-compose/README.md)
- 📐 [Java Agent 架构设计说明](agent/docs/design.md)
- 📑 [Java Agent API 详细参考](agent/docs/api_reference.md)
- 📦 [privacy-java-sdk 核心库文档](privacy-java-sdk/docs/)

---

## 10. 许可证与相关项目

- 本项目采用 **[Apache License 2.0](LICENSE)** 许可证。
- [PrivShield Python](https://github.com/fengzhizi319/PrivShield) —— 原版 Python 隐私计算 Agent
- [privacy-java-sdk](https://github.com/fengzhizi319/privacy-java-sdk) —— 独立 Java 隐私计算核心库
- [privacy-go-sdk](https://github.com/fengzhizi319/privacy-go-sdk) —— 独立 Go 语言隐私计算库

