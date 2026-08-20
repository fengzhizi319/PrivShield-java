# 部署设计文档

## 1. 部署模式

PrivShield Java 支持三种部署模式：

### 1.1 本地开发模式
```
Agent(:8079/:50051) ← Go(:8081) ← Frontend(:5174)
```

### 1.2 Docker Compose 模式
```yaml
services:
  agent:
    image: privshield-java:latest
    ports: ["8079:8079", "50051:50051"]
  backend:
    image: privshield-go:latest
    ports: ["8081:8081"]
  web:
    image: privshield-web:latest
    ports: ["5174:80"]
```

### 1.3 Kubernetes 模式
```
Ingress → Service → Pod(Agent/Backend/Web)
```

## 2. 网络拓扑

```
                    ┌─────────────┐
                    │   Ingress   │
                    │  (TLS 终止) │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────┴─────┐ ┌───┴───┐ ┌─────┴─────┐
        │  Web Pod  │ │ Go    │ │ Agent Pod │
        │  (:5174)  │ │(:8081)│ │(:8079/5051)│
        └───────────┘ └───────┘ └───────────┘
```

## 3. 存储需求

| 组件 | 存储 | 说明 |
|---|---|---|
| Agent | /config/rules/ | YAML 规则文件 |
| Agent | /data/audit/ | 审计日志 |
| Backend | /data/cache/ | 缓存数据 |

## 4. 安全要求

- Agent 间通信使用 mTLS
- 外部访问通过 Ingress TLS
- 密钥通过 Kubernetes Secrets 管理
- 网络策略限制 Pod 间通信
