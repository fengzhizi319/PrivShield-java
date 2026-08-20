# 医疗流水线 — 运维手册

## 1. 部署架构

```
Client → REST(:8079) → MedicalPipeline
                          ├── DynClassificationEngine (分类)
                          ├── MaskingApi (脱敏)
                          └── AuditLogger (审计)
```

## 2. 配置项

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `privacy.medical.enabled` | `true` | 是否启用医疗流水线 |
| `privacy.medical.profile` | `medical` | 分类 profile |
| `privacy.medical.auto-classify` | `true` | 是否自动分类 |
| `privacy.medical.audit-enabled` | `true` | 是否启用审计 |
| `privacy.medical.timeout-ms` | `5000` | 单条处理超时 |

## 3. 监控指标

| 指标名 | 类型 | 说明 |
|---|---|---|
| `privacy_medical_processed_total` | Counter | 处理记录总数 |
| `privacy_medical_duration_seconds` | Histogram | 处理耗时 |
| `privacy_medical_fields_masked_total` | Counter | 脱敏字段总数 |
| `privacy_medical_errors_total` | Counter | 处理错误数 |

## 4. 告警规则

```yaml
groups:
  - name: medical-pipeline
    rules:
      - alert: MedicalPipelineErrorRate
        expr: rate(privacy_medical_errors_total[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
      - alert: MedicalPipelineLatency
        expr: histogram_quantile(0.99, privacy_medical_duration_seconds) > 5.0
        for: 5m
        labels:
          severity: warning
```

## 5. 故障排查

### 处理超时
1. 检查分类引擎（特别是 LLM 层）连通性
2. 增大 `timeout-ms` 配置
3. 考虑关闭 LLM 层，仅使用规则+NER

### 脱敏不完整
1. 检查分类规则覆盖度
2. 确认字段名映射正确
3. 查看审计日志中的未脱敏字段
