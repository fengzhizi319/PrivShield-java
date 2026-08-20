# K-匿名化模块 — 测试文档

## 1. 测试策略

### 单元测试

```java
@Test
void testKAnonymityBasic() {
    KAnonymityApi kano = new KAnonymityApi();
    List<Map<String, Object>> records = createTestRecords(10);
    List<Map<String, Object>> result = kano.anonymize(records, List.of("age", "city"), 3);
    // 验证每个等价类至少 3 条记录
    assertTrue(verifyKProperty(result, List.of("age", "city"), 3));
}

@Test
void testKAnonymityInsufficientRecords() {
    KAnonymityApi kano = new KAnonymityApi();
    List<Map<String, Object>> records = List.of(Map.of("age", 25));
    assertThrows(IllegalArgumentException.class, () ->
        kano.anonymize(records, List.of("age"), 5)
    );
}
```

## 2. 测试用例矩阵

| 场景 | 输入 | 预期 | 优先级 |
|---|---|---|---|
| 基本匿名 | 10条, k=3 | 每组≥3条 | P0 |
| K 值=2 | 数据, k=2 | 每组≥2条 | P0 |
| 记录不足 | 2条, k=5 | 抛异常 | P0 |
| 数值泛化 | age=25,26,27 | 区间化 | P0 |
| 分类泛化 | city 泛化 | 上级类别 | P1 |
| 大数量 | 10000条 | <100ms | P1 |

## 3. 运行测试

```bash
mvn test -pl agent/agent-sdk -Dtest=KAnonymityApiTest
mvn test -pl agent/agent-server -Dtest=KAnoControllerIntegrationTest
```
