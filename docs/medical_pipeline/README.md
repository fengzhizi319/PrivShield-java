# 医疗流水线 (Medical Pipeline)

> 针对医疗健康数据的端到端隐私处理流水线，集成分类分级 + 智能脱敏。

## 功能概述

- **自动分类**: 识别医疗记录中的敏感字段
- **智能脱敏**: 根据分类结果自动选择脱敏策略
- **诊断保留**: 医疗诊断信息保持原样（非 PII）
- **批量处理**: 支持单条记录和批量表格处理

## 快速开始

```bash
curl -X POST http://localhost:8079/v1/medical_pipeline/process_records \
  -H "Content-Type: application/json" \
  -d '{
    "records": [
      {"name": "张三", "phone": "13812345678", "id_card": "110101199001011234", "diagnosis": "糖尿病"}
    ]
  }'
```

**响应:**
```json
{
  "sanitized_data": [{"name": "张*", "phone": "138****5678", "id_card": "110101********1234", "diagnosis": "糖尿病"}],
  "classification_report": [{"max_level": "L1"}]
}
```

## 处理流程

```
原始记录 → 字段识别 → 分类分级 → 脱敏策略选择 → 脱敏执行 → 输出
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [设计文档](design.md) | 流水线架构设计 |
| [API 参考](api_reference.md) | REST 接口定义 |
| [运维指南](ops.md) | 配置说明 |
| [测试指南](testing.md) | 测试方法 |
