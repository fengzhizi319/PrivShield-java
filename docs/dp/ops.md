# 差分隐私模块 — 运维手册

## 1. 部署架构

```
Client → REST(:8079) / gRPC(:50051) → DpApi → Laplace 噪声引擎
                                              ↓
                                        BudgetAccountant (ε 跟踪)
```

## 2. 配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `privacy.dp.default-epsilon` | `1.0` | 默认隐私预算 |
| `privacy.dp.max-epsilon` | `10.0` | 单次查询最大 ε |
| `privacy.dp.budget-enabled` | `false` | 是否启用预算跟踪 |
| `privacy.dp.budget-file` | `config/budget.json` | 预算持久化文件 |

## 3. 监控指标

| 指标名 | 类型 | 说明 |
|---|---|---|
| `privacy_dp_operations_total` | Counter | DP 操作总数 (按 type 分) |
| `privacy_dp_epsilon_consumed` | Gauge | 已消耗 ε 总量 |
| `privacy_dp_noise_ratio` | Histogram | 噪声/真实值比率 |
| `privacy_dp_budget_exhausted_total` | Counter | 预算耗尽次数 |

## 4. 告警规则

```yaml
groups:
  - name: differential-privacy
    rules:
      - alert: BudgetNearlyExhausted
        expr: privacy_dp_epsilon_consumed > 0.8 * privacy_dp_epsilon_budget
        for: 1m
        labels:
          severity: warning
      - alert: BudgetExhausted
        expr: privacy_dp_budget_exhausted_total > 0
        for: 0m
        labels:
          severity: critical
      - alert: HighNoiseRatio
        expr: histogram_quantile(0.95, privacy_dp_noise_ratio) > 0.5
        for: 10m
        labels:
          severity: info
```

## 5. 日志

| 事件 | 级别 | 说明 |
|---|---|---|
| `DpOperationExecuted` | DEBUG | DP 操作完成 |
| `DpBudgetUpdated` | INFO | 预算更新 |
| `DpBudgetExhausted` | WARN | 预算耗尽 |
| `DpInvalidEpsilon` | WARN | ε 值异常 |

## 6. 故障排查

### 噪声过大
1. 检查 ε 值是否过小
2. 确认数据范围 [a,b] 是否合理
3. 考虑增大 ε 或减小数据范围

### 预算提前耗尽
1. 增大总预算
2. 优化查询策略，减少冗余查询
3. 使用组合定理优化 ε 分配
