# 动态分类分级模块 API 参考

## 目录

- [1. Java SDK](#1-java-sdk)
- [2. REST API](#2-rest-api)
- [3. gRPC API](#3-grpc-api)
- [4. 异常与错误码](#4-异常与错误码)

---

## 1. Java SDK

### `DynClassificationEngine.classify`

```java
public ClassificationResult classify(String fieldName, String fieldValue)
```

三层漏斗分类：规则 → NER → LLM。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `fieldName` | `String` | 是 | 字段名 |
| `fieldValue` | `String` | 是 | 字段值 |

**返回值**: `ClassificationResult` 包含 level (L1~L5)、category、confidence、engine 信息。

### `DynClassificationEngine.classifyBatch`

```java
public List<ClassificationResult> classifyBatch(
    List<FieldInfo> fields
)
```

批量分类。

### `DynClassificationEngine.classifyWithProfile`

```java
public ClassificationResult classifyWithProfile(
    String fieldName, String fieldValue, String profileName
)
```

使用指定分类 profile 进行分类。

---

## 2. REST API

### POST `/v1/privacy/dynclassify`

**请求体**:
```json
{
  "field_name": "patient_name",
  "field_value": "张三",
  "profile": "medical"
}
```

**响应**:
```json
{
  "level": "L3",
  "category": "个人身份信息",
  "confidence": 0.95,
  "engine": "rule",
  "description": "姓名属于个人基本身份信息"
}
```

### POST `/v1/privacy/dynclassify/batch`

**请求体**:
```json
{
  "fields": [
    {"field_name": "patient_name", "field_value": "张三"},
    {"field_name": "diagnosis", "field_value": "糖尿病"},
    {"field_name": "phone", "field_value": "13812345678"}
  ]
}
```

### GET `/v1/privacy/dynclassify/profiles`

获取所有可用的分类 profile。

### GET `/v1/privacy/dynclassify/levels`

获取分类级别定义 (L1~L5)。

---

## 3. gRPC API

| RPC 方法 | 请求类型 | 响应类型 | 说明 |
|---|---|---|---|
| `DynClassify` | `DynClassifyRequest` | `DynClassifyResponse` | 单字段分类 |
| `DynClassifyBatch` | `DynClassifyBatchRequest` | `DynClassifyBatchResponse` | 批量分类 |
| `GetProfiles` | `GetProfilesRequest` | `GetProfilesResponse` | 获取 profiles |
| `GetLevels` | `GetLevelsRequest` | `GetLevelsResponse` | 获取级别定义 |

---

## 4. 异常与错误码

| 错误类型 | 触发条件 | HTTP 状态码 |
|---|---|---|
| `ProfileNotFoundException` | 指定 profile 不存在 | 404 |
| `ClassificationFailedException` | 三层均无法分类 | 500 |
| `InvalidFieldException` | 字段名为空 | 400 |
