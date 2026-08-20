# 部署运维手册

## 1. 环境要求

| 组件 | 最低版本 | 推荐版本 |
|---|---|---|
| JDK | 17 | 17.0.8+ |
| Go | 1.21 | 1.21.5+ |
| Node.js | 16.14 | 18.x |
| Docker | 20.10 | 24.x |
| Kubernetes | 1.24 | 1.28+ |

## 2. 健康检查

```bash
# Agent 健康检查
curl http://localhost:8079/actuator/health

# Go Backend 健康检查
curl http://localhost:8081/health

# gRPC 健康检查
grpc_health_check --addr=localhost:50051
```

## 3. 日志管理

| 组件 | 日志位置 | 格式 |
|---|---|---|
| Agent | stdout / /logs/agent.log | JSON (SLF4J) |
| Backend | stdout / /logs/backend.log | JSON (Zap) |
| Web | stdout | 文本 |

## 4. 备份策略

- 规则文件：Git 版本管理
- 审计日志：每日归档到对象存储
- 配置数据：Kubernetes ConfigMap

## 5. 升级流程

```bash
# 滚动升级 (K8s)
kubectl set image deployment/agent agent=privshield-java:v0.2.0 -n privshield
kubectl rollout status deployment/agent -n privshield
```
