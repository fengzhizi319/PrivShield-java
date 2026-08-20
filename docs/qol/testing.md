# 查询混淆模块 — 测试文档

## 1. 测试策略

### 单元测试

```java
@Test
void testObfuscateQueryAddsNoise() {
    QolApi qol = new QolApi();
    String sql = "SELECT * FROM patients WHERE age > 30";
    String result = qol.obfuscateQuery(sql, 0.3);
    assertTrue(result.contains("UNION"));
}

@Test
void testInjectNoiseRecords() {
    QolApi qol = new QolApi();
    List<Map<String, Object>> records = List.of(
        Map.of("name", "张三", "age", 30)
    );
    ObfuscationResult result = qol.obfuscateWithFakeRecords(records, List.of("name"), 0.5);
    assertTrue(result.getRecords().size() > records.size());
}
```

## 2. 测试用例矩阵

| 场景 | 输入 | 预期 | 优先级 |
|---|---|---|---|
| SQL 混淆 | SELECT 语句 | 含 UNION | P0 |
| 噪声注入 | 2条, ratio=0.5 | 增加记录 | P0 |
| 无效 SQL | "INVALID" | 抛异常 | P0 |
| noiseScale=0 | 查询 | 无变化 | P1 |
| noiseScale=1 | 查询 | 最大噪声 | P1 |
| 复杂 SQL | JOIN/子查询 | 正确混淆 | P1 |

## 3. 运行测试

```bash
mvn test -pl agent/agent-sdk -Dtest=QolApiTest
mvn test -pl agent/agent-server -Dtest=QolControllerIntegrationTest
```
