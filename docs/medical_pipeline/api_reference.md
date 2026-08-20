# 医疗流水线模块 API 参考

## 目录

- [1. Java SDK](#1-java-sdk)
- [2. REST API](#2-rest-api)
- [3. 异常与错误码](#3-异常与错误码)

---

## 1. Java SDK

### `MedicalPipeline.process`

```java
public PipelineResult process(MedicalRecord record)
```

执行完整的医疗数据处理流水线：分类 → 脱敏 → 输出。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `record` | `MedicalRecord` | 是 | 医疗记录 |

**返回值**: `PipelineResult` 包含脱敏后记录、分类结果、审计信息。

### `MedicalPipeline.processBatch`

```java
public List<PipelineResult> processBatch(List<MedicalRecord> records)
```

批量处理。

---

## 2. REST API

### POST `/v1/privacy/medical/process`

**请求体**:
```json
{
  "patient_name": "张三",
  "phone": "13812345678",
  "id_card": "110101199001011234",
  "diagnosis": "2型糖尿病",
  "prescription": "二甲双胍 500mg bid",
  "notes": "患者情绪稳定"
}
```

**响应**:
```json
{
  "result": {
    "patient_name": "张*",
    "phone": "138****5678",
    "id_card": "110101********1234",
    "diagnosis": "2型糖尿病",
    "prescription": "二甲双胍 500mg bid",
    "notes": "患者情绪稳定"
  },
  "classifications": [
    {"field": "patient_name", "level": "L3", "confidence": 0.95},
    {"field": "diagnosis", "level": "L4", "confidence": 0.92}
  ],
  "audit": {
    "processed_at": "2026-08-20T10:00:00Z",
    "fields_masked": 3,
    "pipeline_version": "0.1.0"
  }
}
```

### POST `/v1/privacy/medical/process/batch`

批量处理。

---

## 3. 异常与错误码

| 错误类型 | 触发条件 | HTTP 状态码 |
|---|---|---|
| `InvalidRecordException` | 记录为空或缺少必要字段 | 400 |
| `PipelineExecutionException` | 流水线内部错误 | 500 |
| `ClassificationTimeoutException` | 分类超时 | 504 |
