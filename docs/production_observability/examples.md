# 可观测性 — 使用示例

## Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "PrivShield Java Metrics",
    "panels": [
      {"title": "Mask Operations/s", "type": "graph", "expr": "rate(privacy_mask_operations_total[5m])"},
      {"title": "P99 Latency", "type": "graph", "expr": "histogram_quantile(0.99, privacy_mask_duration_seconds)"}
    ]
  }
}
```

## 告警配置

```yaml
# alerting-rules.yaml
groups:
  - name: privshield
    rules:
      - alert: HighErrorRate
        expr: rate(privacy_mask_errors_total[5m]) / rate(privacy_mask_operations_total[5m]) > 0.05
        for: 5m
```
