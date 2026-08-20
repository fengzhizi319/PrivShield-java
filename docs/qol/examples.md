# 查询混淆使用示例

## 1. Java SDK 示例

### 1.1 SQL 查询混淆

```java
QolApi qol = new QolApi();

String sql = "SELECT * FROM patients WHERE diagnosis='diabetes'";
String obfuscated = qol.obfuscateQuery(sql, 0.3);
// 原始查询 + UNION 注入噪声记录
```

### 1.2 数据级噪声注入

```java
List<Map<String, Object>> records = List.of(
    Map.of("name", "张三", "age", 30, "diagnosis", "Flu"),
    Map.of("name", "李四", "age", 45, "diagnosis", "Diabetes")
);

ObfuscationResult result = qol.obfuscateWithFakeRecords(
    records, List.of("name"), 0.2
);
// 原始 2 条 + 噪声 1 条 = 3 条
```

## 2. REST API 示例

```bash
# SQL 查询混淆
curl -X POST http://localhost:8079/v1/privacy/qol/obfuscate \
  -H "Content-Type: application/json" \
  -d '{"query": "SELECT * FROM patients", "noise_scale": 0.3}'

# 数据级噪声注入
curl -X POST http://localhost:8079/v1/privacy/qol/inject \
  -H "Content-Type: application/json" \
  -d '{
    "records": [{"name": "张三", "age": 30}],
    "sensitive_columns": ["name"],
    "noise_ratio": 0.2
  }'
```

## 3. 最佳实践

1. **noiseScale 选择**: 0.1~0.3 适合一般场景，0.5+ 用于高敏数据
2. **噪声记录**: 伪造记录应使用合理但虚假的数据
3. **性能影响**: 噪声注入会增加结果集大小，注意下游系统承载能力
4. **审计**: 记录每次混淆操作的参数和结果
