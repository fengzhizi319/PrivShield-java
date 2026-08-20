# 网关负载均衡 — 使用示例

## 配置示例

```yaml
gateway:
  grpc:
    listen: ":443"
    backends:
      - addr: "agent-1:50051"
        weight: 3
      - addr: "agent-2:50051"
        weight: 1
  rest:
    listen: ":80"
    backends:
      - addr: "agent-1:8079"
      - addr: "agent-2:8079"
  health_check:
    interval: 10s
    timeout: 3s
    unhealthy_threshold: 3
```
