# 动态分类分级 — 测试文档

## 1. 测试策略

### 单元测试

```java
@Test
void testRuleEngineClassifyPhone() {
    DynClassificationEngine engine = new DynClassificationEngine();
    ClassificationResult result = engine.classify("phone", "13812345678");
    assertEquals("L3", result.getLevel());
    assertEquals("rule", result.getEngine());
    assertTrue(result.getConfidence() >= 0.9);
}

@Test
void testRuleEngineClassifyName() {
    DynClassificationEngine engine = new DynClassificationEngine();
    ClassificationResult result = engine.classify("patient_name", "张三");
    assertEquals("L3", result.getLevel());
}

@Test
void testBatchClassify() {
    DynClassificationEngine engine = new DynClassificationEngine();
    List<FieldInfo> fields = List.of(
        new FieldInfo("phone", "13812345678"),
        new FieldInfo("name", "张三")
    );
    List<ClassificationResult> results = engine.classifyBatch(fields);
    assertEquals(2, results.size());
}
```

## 2. 测试用例矩阵

| 场景 | 字段 | 预期级别 | 引擎 | 优先级 |
|---|---|---|---|---|
| 手机号 | phone | L3 | rule | P0 |
| 姓名 | name | L3 | rule | P0 |
| 身份证 | id_card | L4 | rule | P0 |
| 诊断 | diagnosis | L4 | rule/ner | P0 |
| 未知字段 | random | L2 | fallback | P1 |
| 批量分类 | 多字段 | 各自分类 | - | P0 |
| Profile 切换 | medical | 医疗规则 | - | P1 |
| 置信度阈值 | 边界值 | 正确降级 | - | P1 |

## 3. 运行测试

```bash
mvn test -pl agent/agent-sdk -Dtest=DynClassificationEngineTest
mvn test -pl agent/agent-server -Dtest=DynClassifyControllerIntegrationTest
```
