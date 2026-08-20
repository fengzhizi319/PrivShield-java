# 查询混淆模块 API 参考

## 目录

- [1. Java SDK](#1-java-sdk)
- [2. REST API](#2-rest-api)
- [3. gRPC API](#3-grpc-api)
- [4. 异常与错误码](#4-异常与错误码)

---

## 1. Java SDK

### `QolApi.obfuscateQuery`

```java
public String obfuscateQuery(String sql, double noiseScale)
```

对 SQL 查询进行混淆处理，注入噪声记录。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `sql` | `String` | 是 | 原始 SQL 查询 |
| `noiseScale` | `double` | 是 | 噪声强度 (0.0~1.0) |

### `QolApi.obfuscateWithFakeRecords`

```java
public ObfuscationResult obfuscateWithFakeRecords(
    List<Map<String, Object>> records,
    List<String> sensitiveColumns,
    double noiseRatio
)
```

在数据集中注入伪造记录。

---

## 2. REST API

### POST `/v1/privacy/qol/obfuscate`

**请求体**:
```json
{
  "query": "SELECT * FROM patients WHERE diagnosis='diabetes'",
  "noise_scale": 0.3
}
```

**响应**:
```json
{
  "obfuscated_query": "SELECT * FROM patients WHERE diagnosis='diabetes' UNION SELECT ...",
  "noise_records_added": 5
}
```

### POST `/v1/privacy/qol/inject`

**请求体**:
```json
{
  "records": [{"name": "张三", "age": 30}],
  "sensitive_columns": ["name"],
  "noise_ratio": 0.2
}
```

---

## 3. gRPC API

| RPC 方法 | 请求类型 | 响应类型 | 说明 |
|---|---|---|---|
| `ObfuscateQuery` | `ObfuscateQueryRequest` | `ObfuscateQueryResponse` | SQL 查询混淆 |
| `InjectNoise` | `InjectNoiseRequest` | `InjectNoiseResponse` | 噪声注入 |

---

## 4. 异常与错误码

| 错误类型 | 触发条件 | HTTP 状态码 |
|---|---|---|
| `SqlParseException` | SQL 语法错误 | 400 |
| `UnsupportedQueryException` | 不支持的 SQL 类型 | 400 |
| `NoiseScaleOutOfRange` | noiseScale 不在 [0,1] | 400 |
