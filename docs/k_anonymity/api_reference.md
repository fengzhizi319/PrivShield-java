# K-匿名化模块 API 参考

## 目录

- [1. Java SDK](#1-java-sdk)
- [2. REST API](#2-rest-api)
- [3. gRPC API](#3-grpc-api)
- [4. 异常与错误码](#4-异常与错误码)

---

## 1. Java SDK

### `KAnonymityApi.anonymize`

```java
public List<Map<String, Object>> anonymize(
    List<Map<String, Object>> records,
    List<String> quasiIdentifiers,
    int k
)
```

对记录集进行 K-匿名化处理。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `records` | `List<Map<String, Object>>` | 是 | 原始记录集 |
| `quasiIdentifiers` | `List<String>` | 是 | 准标识符字段名列表 |
| `k` | `int` | 是 | K 值（每组至少 k 条） |

**返回值**: 泛化后的记录集

### `KAnonymityApi.anonymizeTable`

```java
public KAnonymityTable anonymizeTable(
    List<String> headers,
    List<List<String>> rows,
    List<String> quasiIdentifiers,
    int k
)
```

表格级 K-匿名化。

---

## 2. REST API

### POST `/v1/privacy/kano`

**请求体**:
```json
{
  "records": [
    {"age": 25, "gender": "M", "city": "Beijing", "salary": 15000},
    {"age": 26, "gender": "M", "city": "Beijing", "salary": 16000},
    {"age": 30, "gender": "F", "city": "Shanghai", "salary": 20000}
  ],
  "quasi_identifiers": ["age", "gender", "city"],
  "k": 2
}
```

**响应**:
```json
{
  "result": [
    {"age": "25-26", "gender": "M", "city": "Beijing", "salary": 15000},
    {"age": "25-26", "gender": "M", "city": "Beijing", "salary": 16000},
    {"age": "30+", "gender": "F", "city": "Shanghai", "salary": 20000}
  ]
}
```

---

## 3. gRPC API

| RPC 方法 | 请求类型 | 响应类型 | 说明 |
|---|---|---|---|
| `KAnonymity` | `KAnonymityRequest` | `KAnonymityResponse` | K-匿名化 |
| `KAnonymityTable` | `KAnonymityTableRequest` | `KAnonymityTableResponse` | 表格级匿名 |

---

## 4. 异常与错误码

| 错误类型 | 触发条件 | HTTP 状态码 | gRPC 状态码 |
|---|---|---|---|
| `IllegalArgumentException` | k ≤ 0 或准标识符为空 | 400 | `INVALID_ARGUMENT` |
| `InsufficientRecordsException` | 记录数 < k | 400 | `FAILED_PRECONDITION` |
