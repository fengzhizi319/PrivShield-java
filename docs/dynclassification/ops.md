# 动态分类分级 — 运维手册

## 1. 部署架构

```
Client → REST(:8079) → DynClassificationEngine
                          ├── L1: RuleEngine (YAML 规则)
                          ├── L2: NerAdapter (本地 NER 模型)
                          └── L3: LlmAdapter (远程 LLM API)
```

## 2. 配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `privacy.dyn.rules-dir` | `rules/` | 规则文件目录 |
| `privacy.dyn.profile` | `medical` | 默认分类 profile |
| `privacy.dyn.ner-enabled` | `true` | 是否启用 NER 层 |
| `privacy.dyn.llm-enabled` | `false` | 是否启用 LLM 层 |
| `privacy.dyn.llm-endpoint` | (env) | LLM API 地址 |
| `privacy.dyn.llm-api-key` | (env) | LLM API Key |
| `privacy.dyn.l1-threshold` | `0.9` | L1 置信度阈值 |
| `privacy.dyn.l2-threshold` | `0.7` | L2 置信度阈值 |

## 3. 监控指标

| 指标名 | 类型 | 说明 |
|---|---|---|
| `privacy_dyn_classify_total` | Counter | 分类操作总数 |
| `privacy_dyn_classify_by_engine` | Counter | 按引擎层分类 (rule/ner/llm) |
| `privacy_dyn_classify_duration_seconds` | Histogram | 分类耗时 |
| `privacy_dyn_fallback_total` | Counter | 降级次数 (L1→L2, L2→L3) |

## 4. 告警规则

```yaml
groups:
  - name: dynclassification
    rules:
      - alert: HighLlmFallbackRate
        expr: rate(privacy_dyn_fallback_total{to="llm"}[10m]) > 0.3
        for: 10m
        labels:
          severity: warning
      - alert: LlmLatencyHigh
        expr: histogram_quantile(0.99, privacy_dyn_classify_duration_seconds{engine="llm"}) > 5.0
        for: 5m
        labels:
          severity: warning
```

## 5. 故障排查

### 分类准确率低
1. 检查规则文件是否覆盖常见字段
2. 确认 NER 模型已正确加载
3. 检查 LLM prompt 模板

### LLM 调用超时
1. 检查 LLM API 连通性
2. 确认 API Key 有效
3. 考虑增加超时或启用缓存
