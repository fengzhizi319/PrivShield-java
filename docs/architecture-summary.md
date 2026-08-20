# PrivShield Java — 架构概览

## 定位

PrivShield Java 是 [PrivShield Python](https://github.com/fengzhizi319/PrivShield) 的 Java 实现，提供企业级数据隐私计算能力，作为 **Privacy Governance Sidecar** 运行。

## 核心能力

| 能力 | 说明 | 协议 |
|------|------|------|
| 数据脱敏 (Masking) | 姓名/手机/身份证/邮箱智能掩码 | gRPC |
| 差分隐私 (DP) | Laplace 噪声注入: Count/Sum/Mean | gRPC |
| K-匿名化 | 准标识符泛化，Mondrian 分割 | gRPC |
| 查询混淆 (QoL) | SQL 查询混淆 + 噪声记录注入 | gRPC |
| 动态分类分级 | 三层漏斗: 规则→NER→LLM | REST |
| 医疗流水线 | 分类 + 脱敏一体化处理 | REST |

## 技术栈

```
Java 17 + Spring Boot 3.3.5 + gRPC 1.62.2
├── agent-sdk       隐私计算 SDK (Masking/DP/KAnon/QoL/DynClassify)
├── agent-server    gRPC (33 RPC) + REST (13 Controller) 服务端
└── privacy-java-sdk 独立工业级隐私库 (P0/P1/P2 全面升级)

Go 1.21 + Gin
└── console/backend-go  gRPC 代理 + REST 转发

React 18 + Vite + TypeScript + TailwindCSS
└── console/web  前端控制台 UI
```

## 三端联调架构

```
Frontend (:5174) ──Vite Proxy──▶ Go Backend (:8081)
                                      │
                          ┌───────────┴───────────┐
                          ▼                       ▼
                    gRPC (:50051)          REST (:8079)
                          │                       │
                          └───────────┬───────────┘
                                      ▼
                              Java Agent (SDK)
```

- **gRPC 路径**: 高频隐私原语 (Mask/DP/KAnon/QoL)
- **REST 路径**: 复杂编排功能 (DynClassification/MedicalPipeline)

## 端口分配

| 服务 | 端口 | 协议 |
|------|------|------|
| Frontend (Vite dev) | 5174 | HTTP |
| Go Backend | 8081 | HTTP |
| Java Agent REST | 8079 | HTTP |
| Java Agent gRPC | 50051 | gRPC |

## 与 Python 版本对比

| 维度 | Python (PrivShield) | Java (PrivShield-java) |
|------|---------------------|------------------------|
| 语言 | Python 3.10+ | Java 17 |
| 框架 | FastAPI + Uvicorn | Spring Boot 3.3.5 |
| gRPC | grpcio | grpc-spring-boot-starter |
| 性能 | 中等 (GIL 限制) | 高 (JIT + 多线程) |
| 部署 | pip + Docker | JAR + Docker |
| 适用场景 | 快速原型/研究 | 企业级生产 |
