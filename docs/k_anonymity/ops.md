# K-匿名化模块 — 运维手册

## 1. 部署架构

```
Client → REST(:8079) / gRPC(:50051) → KAnonymityApi → Mondrian 分割引擎
```

## 2. 配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `privacy.kano.default-k` | `5` | 默认 K 值 |
| `privacy.kano.min-k` | `2` | 最小允许 K 值 |
| `privacy.kano.max-k` | `100` | 最大允许 K 值 |
| `privacy.kano.mondrian-threshold` | `1000` | 触发 Mondrian 的记录数阈值 |

## 3. 监控指标

| 指标名 | 类型 | 说明 |
|---|---|---|
| `privacy_kano_operations_total` | Counter | K-匿名操作总数 |
| `privacy_kano_records_processed` | Counter | 处理记录总数 |
| `privacy_kano_generalization_ratio` | Histogram | 泛化比率 |
| `privacy_kano_operation_duration_seconds` | Histogram | 操作耗时 |

## 4. 告警规则

```yaml
groups:
  - name: k-anonymity
    rules:
      - alert: HighGeneralizationRatio
        expr: histogram_quantile(0.95, privacy_kano_generalization_ratio) > 0.8
        for: 10m
        labels:
          severity: warning
      - alert: KAnonymityLatencyHigh
        expr: histogram_quantile(0.99, privacy_kano_operation_duration_seconds) > 5.0
        for: 5m
        labels:
          severity: warning
```

## 5. 故障排查

### 泛化过度
1. 增大 K 值或减少准标识符数量
2. 检查数据分布是否均匀
3. 考虑使用更细粒度的泛化策略

### 性能问题
1. 大数据集确认 Mondrian 算法已启用
2. 检查内存使用（大量记录时）
3. 考虑分批处理
