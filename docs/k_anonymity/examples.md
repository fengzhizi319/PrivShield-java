# K-匿名化使用示例

## 1. Java SDK 示例

### 1.1 基本 K-匿名化

```java
KAnonymityApi kano = new KAnonymityApi();

List<Map<String, Object>> records = List.of(
    Map.of("age", 25, "gender", "M", "city", "Beijing", "salary", 15000),
    Map.of("age", 26, "gender", "M", "city", "Beijing", "salary", 16000),
    Map.of("age", 30, "gender", "F", "city", "Shanghai", "salary", 20000),
    Map.of("age", 31, "gender", "F", "city", "Shanghai", "salary", 21000)
);

List<Map<String, Object>> result = kano.anonymize(
    records, List.of("age", "gender", "city"), 2
);
// age 被泛化为区间，如 "25-26"
```

### 1.2 表格级匿名

```java
List<String> headers = List.of("age", "gender", "city", "diagnosis");
List<List<String>> rows = List.of(
    List.of("25", "M", "Beijing", "Flu"),
    List.of("26", "M", "Beijing", "Cold"),
    List.of("30", "F", "Shanghai", "Diabetes")
);

KAnonymityTable table = kano.anonymizeTable(headers, rows, List.of("age", "gender", "city"), 2);
```

## 2. REST API 示例

```bash
curl -X POST http://localhost:8079/v1/privacy/kano \
  -H "Content-Type: application/json" \
  -d '{
    "records": [
      {"age": 25, "gender": "M", "city": "Beijing"},
      {"age": 26, "gender": "M", "city": "Beijing"},
      {"age": 30, "gender": "F", "city": "Shanghai"},
      {"age": 31, "gender": "F", "city": "Shanghai"}
    ],
    "quasi_identifiers": ["age", "gender", "city"],
    "k": 2
  }'
```

## 3. 最佳实践

1. **K 值选择**: 医疗数据推荐 k ≥ 5，一般场景 k ≥ 3
2. **准标识符**: 仅选择可能关联到个人的字段，敏感属性不作为准标识符
3. **数据量**: 确保记录数远大于 k，否则泛化过度
4. ** Mondrian 分割**: 大数据集自动使用 Mondrian 算法提高效率
