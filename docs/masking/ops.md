# 数据脱敏模块 — 运维手册

## 1. 部署架构

```
Client → REST(:8079) / gRPC(:50051) → MaskingApi → 规则引擎
```

## 2. 配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `privacy.masking.enabled` | `true` | 是否启用脱敏 |
| `privacy.masking.rules-path` | `rules/masking.yaml` | 脱敏规则文件路径 |
| `privacy.masking.hmac-salt` | (env) | HMAC 盐值，建议通过环境变量注入 |

## 3. 监控指标

| 指标名 | 类型 | 说明 |
|---|---|---|
| `privacy_mask_operations_total` | Counter | 脱敏操作总数 |
| `privacy_mask_operation_duration_seconds` | Histogram | 脱敏操作耗时 |
| `privacy_mask_errors_total` | Counter | 脱敏错误总数 |

## 4. 告警规则

```yaml
groups:
  - name: masking
    rules:
      - alert: HighMaskErrorRate
        expr: rate(privacy_mask_errors_total[5m]) / rate(privacy_mask_operations_total[5m]) > 0.05
        for: 5m
        labels:
          severity: warning
      - alert: MaskLatencyHigh
        expr: histogram_quantile(0.99, privacy_mask_operation_duration_seconds) > 0.1
        for: 5m
        labels:
          severity: warning
```

## 5. 日志

脱敏模块日志使用 SLF4J，关键日志事件：

| 事件 | 级别 | 说明 |
|---|---|---|
| `MaskingRuleLoaded` | INFO | 规则加载成功 |
| `MaskingRuleLoadFailed` | ERROR | 规则加载失败 |
| `MaskOperationFailed` | WARN | 单次脱敏失败 |

## 6. 故障排查

### 脱敏结果不符合预期
1. 检查 `rules/masking.yaml` 中字段名匹配规则
2. 确认正则模式是否正确
3. 查看日志中的规则匹配详情

### 性能下降
1. 检查批量接口是否被正确使用
2. 确认规则文件未包含过多冗余规则
3. 监控 P99 延迟指标
