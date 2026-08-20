# 数据脱敏使用示例

## 1. Java SDK 示例

### 1.1 单字段脱敏

```java
MaskingApi masking = new MaskingApi();

System.out.println(masking.maskValue("mobile", "13812345678"));
// 138****5678

System.out.println(masking.maskValue("id_card", "110101199001011234"));
// 110101********1234

System.out.println(masking.maskValue("name", "张三丰"));
// 张**丰
```

### 1.2 整记录脱敏

```java
Map<String, Object> record = Map.of(
    "mobile", "13812345678",
    "name", "张三丰",
    "id_card", "110101199001011234",
    "age", 30
);
Map<String, Object> result = masking.maskRecord(record);
// {mobile=138****5678, name=张**丰, id_card=110101********1234, age=30}
```

### 1.3 批量字段脱敏

```java
List<String> result = masking.maskBatch(
    List.of("mobile", "name", "id_card"),
    List.of("13812345678", "张三丰", "110101199001011234")
);
// [138****5678, 张**丰, 110101********1234]
```

### 1.4 HMAC 哈希

```java
String hash = masking.hashValue("user_12345", "my_secret_salt");
// "aB3dE5gH7jK9mN1p"
```

## 2. REST API 示例

```bash
# 单字段脱敏
curl -X POST http://localhost:8079/v1/privacy/mask \
  -H "Content-Type: application/json" \
  -d '{"value": "13812345678", "field_name": "mobile"}'
# {"result": "138****5678"}

# 整记录脱敏
curl -X POST http://localhost:8079/v1/privacy/mask/record \
  -H "Content-Type: application/json" \
  -d '{"record": {"mobile": "13812345678", "name": "张三丰"}}'
# {"result": {"mobile": "138****5678", "name": "张**丰"}}

# 批量脱敏
curl -X POST http://localhost:8079/v1/privacy/mask/batch \
  -H "Content-Type: application/json" \
  -d '{"field_names": ["mobile", "name"], "values": ["13812345678", "张三丰"]}'
# {"results": ["138****5678", "张**丰"]}
```

## 3. 最佳实践

1. **小数据量**（<100条）：使用 `maskValue` 或 `maskRecord`
2. **中等数据量**（100-1000条）：使用 `maskBatch`
3. **大数据量**（>1000条）：使用批量接口 + 分批处理
4. **一致性需求**：使用 `hashValue` 保证相同输入得到相同输出
5. **生产环境**：启用 TLS + 认证 + 速率限制
