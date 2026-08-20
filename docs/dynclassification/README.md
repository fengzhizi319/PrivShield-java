# 动态分类分级 (DynClassification)

> 三层漏斗架构：规则引擎 → NER 模型 → LLM 大模型，实现数据字段的高精度自动分类分级。

## 架构设计

```
输入字段
  │
  ▼
┌─────────────────┐
│ L1: 规则引擎     │ ← 正则 + YAML 规则，速度最快
│ confidence ≥ 0.9│──→ 输出
└────────┬────────┘
         │ < 0.9
         ▼
┌─────────────────┐
│ L2: NER 模型     │ ← 本地 NER 模型，识别实体类型
│ confidence ≥ 0.7│──→ 输出
└────────┬────────┘
         │ < 0.7
         ▼
┌─────────────────┐
│ L3: LLM 大模型   │ ← 调用 LLM API，最高精度
│ 任意 confidence  │──→ 输出
└─────────────────┘
```

## 快速开始

```bash
curl -X POST http://localhost:8079/v1/dynclassification/eval \
  -H "Content-Type: application/json" \
  -d '{"field_name": "name", "value": "张三", "domain": "medical"}'
# 响应: {"max_level": "L1", "confidence": 0.95, "engine_layer": "rule"}
```

## 支持领域

| 领域 | 说明 | 规则文件 |
|------|------|----------|
| medical | 医疗健康 | rules/domains/medical.yaml |
| finance | 金融 | rules/domains/finance.yaml |
| general-pii | 通用 PII | rules/domains/general-pii.yaml |
| gd_health | 广东健康 | rules/domains/gd_health.yaml |

## 文档索引

| 文档 | 说明 |
|------|------|
| [设计文档](design.md) | 三层漏斗详细设计 |
| [API 参考](api_reference.md) | REST 接口定义 |
| [规则解析指南](rule_parsing_guide.md) | YAML 规则编写指南 |
| [运维指南](ops.md) | 配置与调优 |
| [测试指南](testing.md) | 测试方法 |
