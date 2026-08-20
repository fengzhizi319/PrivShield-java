# 查询混淆模块 — 运维手册

## 1. 部署架构

```
Client → REST(:8079) / gRPC(:50051) → QolApi → SQL 解析器 + 噪声生成器
```

## 2. 配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `privacy.qol.enabled` | `true` | 是否启用查询混淆 |
| `privacy.qol.default-noise-scale` | `0.3` | 默认噪声强度 |
| `privacy.qol.max-noise-ratio` | `0.5` | 最大噪声比率 |
| `privacy.qol.supported-dialects` | `mysql,postgresql` | 支持的 SQL 方言 |

## 3. 监控指标

| 指标名 | 类型 | 说明 |
|---|---|---|
| `privacy_qol_operations_total` | Counter | 混淆操作总数 |
| `privacy_qol_noise_records_total` | Counter | 注入噪声记录总数 |
| `privacy_qol_parse_errors_total` | Counter | SQL 解析错误数 |

## 4. 告警规则

```yaml
groups:
  - name: qol
    rules:
      - alert: HighSqlParseErrorRate
        expr: rate(privacy_qol_parse_errors_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
```

## 5. 故障排查

### SQL 解析失败
1. 检查 SQL 方言是否受支持
2. 确认 SQL 语法正确
3. 查看日志中的解析错误详情

### 噪声质量差
1. 调整 noiseScale 参数
2. 检查噪声记录生成逻辑
3. 确认敏感列配置正确
