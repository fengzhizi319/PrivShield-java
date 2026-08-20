# 生产可观测性

> PrivShield Java 的可观测性体系，覆盖日志 (Logging)、指标 (Metrics)、追踪 (Tracing) 三大支柱。

## 架构

```
Agent → SLF4J/Logback → JSON 日志 → 日志平台
Agent → Micrometer → Prometheus → Grafana
Agent → OpenTelemetry → Jaeger/Zipkin
```

## 核心能力

- **结构化日志**: JSON 格式，含 traceId/spanId
- **Prometheus 指标**: 自动暴露 Actuator 端点
- **分布式追踪**: OpenTelemetry 集成
- **健康检查**: Spring Boot Actuator

## 快速开始

```bash
# 查看指标
curl http://localhost:8079/actuator/metrics

# 查看健康状态
curl http://localhost:8079/actuator/health

# Prometheus 格式
curl http://localhost:8079/actuator/prometheus
```
