# 医疗流水线 — 设计文档

## 1. 流水线阶段

### Stage 1: 字段识别

识别输入记录中的字段类型：
- PII 字段: 姓名、手机、身份证、地址
- 医疗字段: 诊断、处方、检验结果
- 普通字段: 年龄、性别

### Stage 2: 分类分级

对每个字段调用 DynClassification 引擎：
- PII 字段 → L3-L4 (敏感/高敏)
- 医疗字段 → L4-L5 (高敏/极敏)
- 普通字段 → L1-L2 (公开/内部)

### Stage 3: 脱敏执行

根据分类结果选择脱敏策略：
- L3: 部分掩码 (如手机保留前3后4)
- L4: 强掩码 (如身份证保留前6后4)
- L5: 保留原值 (如诊断信息，需保留医疗价值)

## 2. 类设计

```
MedicalPipelineController
├── processRecords(records) → MedicalResult
│   ├── classify(record) → ClassificationReport
│   ├── sanitize(record, tags) → SanitizedRecord
│   └── buildReport(results) → MedicalResult
```
