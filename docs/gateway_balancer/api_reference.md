# 网关负载均衡 — API 参考

## REST 代理

| 方法 | 路径 | 后端 |
|---|---|---|
| ALL | /v1/privacy/* | Agent REST(:8079) |

## gRPC 代理

| RPC | 后端 |
|---|---|
| All PrivacyService RPCs | Agent gRPC(:50051) |

## 管理 API

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /health | 网关健康检查 |
| GET | /metrics | Prometheus 指标 |
| GET | /api/backends | 后端列表 |
