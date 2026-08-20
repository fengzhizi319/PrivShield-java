# 网关负载均衡 — 运维手册

## 监控指标

| 指标 | 说明 |
|---|---|
| `gateway_requests_total` | 请求总数 |
| `gateway_backend_health` | 后端健康状态 |
| `gateway_latency_seconds` | 请求延迟 |

## 告警

- 后端全部不健康 → Critical
- P99 延迟 > 1s → Warning
