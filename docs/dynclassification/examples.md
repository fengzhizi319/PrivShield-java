# 动态分类分级使用示例

## 1. Java SDK 示例

### 1.1 单字段分类

```java
DynClassificationEngine engine = new DynClassificationEngine();

ClassificationResult result = engine.classify("patient_name", "张三");
System.out.println(result.getLevel());       // L3
System.out.println(result.getCategory());    // 个人身份信息
System.out.println(result.getConfidence());  // 0.95
System.out.println(result.getEngine());      // rule
```

### 1.2 批量分类

```java
List<FieldInfo> fields = List.of(
    new FieldInfo("patient_name", "张三"),
    new FieldInfo("diagnosis", "2型糖尿病"),
    new FieldInfo("phone", "13812345678"),
    new FieldInfo("address", "北京市朝阳区XX路XX号")
);

List<ClassificationResult> results = engine.classifyBatch(fields);
for (ClassificationResult r : results) {
    System.out.printf("%s → %s (%s, confidence=%.2f, by %s)%n",
        r.getFieldName(), r.getLevel(), r.getCategory(),
        r.getConfidence(), r.getEngine());
}
```

### 1.3 使用自定义 Profile

```java
ClassificationResult result = engine.classifyWithProfile(
    "medical_record", "患者张三，诊断为2型糖尿病", "medical"
);
```

## 2. REST API 示例

```bash
# 单字段分类
curl -X POST http://localhost:8079/v1/privacy/dynclassify \
  -H "Content-Type: application/json" \
  -d '{"field_name": "patient_name", "field_value": "张三", "profile": "medical"}'

# 批量分类
curl -X POST http://localhost:8079/v1/privacy/dynclassify/batch \
  -H "Content-Type: application/json" \
  -d '{"fields": [
    {"field_name": "name", "field_value": "张三"},
    {"field_name": "phone", "field_value": "13812345678"}
  ]}'

# 获取分类级别
curl http://localhost:8079/v1/privacy/dynclassify/levels

# 获取可用 profiles
curl http://localhost:8079/v1/privacy/dynclassify/profiles
```

## 3. 三层漏斗效果对比

| 字段 | L1 规则 | L2 NER | L3 LLM |
|---|---|---|---|
| phone=138xxx | ✅ 直接匹配 (0.99) | - | - |
| diagnosis=糖尿病 | ✅ 医疗词典 (0.95) | - | - |
| note="患者情绪低落" | ❌ 无匹配 | ❌ 无实体 | ✅ 心理健康 (0.82) |
