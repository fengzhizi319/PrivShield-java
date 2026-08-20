# 数据脱敏模块 API 参考

## 目录

- [1. Java SDK](#1-java-sdk)
- [2. REST API](#2-rest-api)
- [3. gRPC API](#3-grpc-api)
- [4. 异常与错误码](#4-异常与错误码)

---

## 1. Java SDK

### `MaskingApi.maskValue`

```java
public String maskValue(String fieldName, String value)
```

根据字段名推断敏感类型并脱敏。

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `fieldName` | `String` | 是 | 字段名（如 "mobile", "name"） |
| `value` | `String` | 是 | 原始值 |

**返回值**: 脱敏后的字符串

### `MaskingApi.maskRecord`

```java
public Map<String, Object> maskRecord(Map<String, Object> record)
```

对整条记录的多个字段进行脱敏。

### `MaskingApi.maskBatch`

```java
public List<String> maskBatch(List<String> fieldNames, List<String> values)
```

批量字段脱敏。

### `MaskingApi.hashValue`

```java
public String hashValue(String value, String salt)
```

HMAC-SHA256 哈希，用于需要一致性映射的场景。

---

## 2. REST API

### POST `/v1/privacy/mask`

单字段脱敏。

**请求体**:
```json
{
  "value": "13812345678",
  "field_name": "mobile"
}
```

**响应**:
```json
{
  "result": "138****5678"
}
```

### POST `/v1/privacy/mask/record`

整记录脱敏。

**请求体**:
```json
{
  "record": {
    "mobile": "13812345678",
    "name": "张三丰",
    "id_card": "110101199001011234"
  }
}
```

### POST `/v1/privacy/mask/batch`

批量字段脱敏。

**请求体**:
```json
{
  "field_names": ["mobile", "name"],
  "values": ["13812345678", "张三丰"]
}
```

### POST `/v1/privacy/hash`

HMAC 哈希。

**请求体**:
```json
{
  "value": "user_12345",
  "salt": "my_secret_salt"
}
```

---

## 3. gRPC API

### 接口列表

| RPC 方法 | 请求类型 | 响应类型 | 说明 |
|---|---|---|---|
| `Mask` | `MaskRequest` | `MaskResponse` | 单字段脱敏 |
| `MaskRecord` | `MaskRecordRequest` | `MaskRecordResponse` | 整记录脱敏 |
| `MaskBatch` | `MaskBatchRequest` | `MaskBatchResponse` | 批量字段脱敏 |
| `Hash` | `HashRequest` | `HashResponse` | HMAC 哈希 |

### Java gRPC 客户端示例

```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .build();
PrivacyServiceGrpc.PrivacyServiceBlockingStub stub =
    PrivacyServiceGrpc.newBlockingStub(channel);

// 单字段脱敏
MaskResponse response = stub.mask(MaskRequest.newBuilder()
    .setFieldName("mobile")
    .setValue("13812345678")
    .build());
// response.getResult() → "138****5678"
```

### gRPC vs REST 选择建议

| 维度 | gRPC | REST |
|---|---|---|
| 性能 | Protobuf 二进制，极高 | JSON 文本，中等 |
| 类型安全 | 强类型 | 弱类型 |
| 适用场景 | 内部微服务、高性能 | 前端API、公开接口 |

---

## 4. 异常与错误码

| 错误类型 | 触发条件 | HTTP 状态码 | gRPC 状态码 |
|---|---|---|---|
| `IllegalArgumentException` | 参数为空或长度不匹配 | 400 | `INVALID_ARGUMENT` |
| `AuthenticationException` | 认证失败 | 401 | `UNAUTHENTICATED` |
| `RateLimitExceededException` | 速率限制超限 | 429 | `RESOURCE_EXHAUSTED` |

### 最佳实践

1. 调用前验证字段名和值的长度一致性
2. HMAC 盐值通过环境变量传递，不要硬编码
3. 生产环境启用 TLS、认证和速率限制
4. 大数据量（>1000条）使用批量接口
5. 监控 `privacy_mask_operations_total` 指标
