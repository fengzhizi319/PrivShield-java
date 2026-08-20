# 差分隐私模块 — 测试文档

## 1. 测试策略

### 单元测试

```java
@Test
void testDpCountReturnsApproximateResult() {
    DpApi dp = new DpApi();
    List<Integer> values = List.of(1, 2, 3, 4, 5);
    double result = dp.dpCount(values, 1.0);
    // 真实值 = 5，结果应在合理范围内
    assertTrue(result > 3.0 && result < 7.0);
}

@Test
void testDpSumWithLargeEpsilon() {
    DpApi dp = new DpApi();
    List<Double> values = List.of(10.0, 20.0, 30.0);
    double result = dp.dpSum(values, 100.0);
    // ε 很大时噪声极小
    assertEquals(60.0, result, 5.0);
}

@Test
void testDpMeanSymmetry() {
    DpApi dp = new DpApi();
    List<Double> values = List.of(0.0, 0.0, 0.0, 0.0, 0.0);
    double result = dp.dpMean(values, 1.0);
    // 均值为0，噪声应围绕0对称
    assertTrue(Math.abs(result) < 5.0);
}
```

## 2. 测试用例矩阵

| 场景 | 输入 | 预期 | 优先级 |
|---|---|---|---|
| DP Count 基本 | [1,2,3,4,5], ε=1.0 | ≈5 + noise | P0 |
| DP Sum 基本 | [10,20,30], ε=0.5 | ≈60 + noise | P0 |
| DP Mean 基本 | [10,20,30,40,50], ε=1.0 | ≈30 + noise | P0 |
| 大 ε 低噪声 | data, ε=100.0 | 接近真实值 | P0 |
| 小 ε 高噪声 | data, ε=0.01 | 偏离较大 | P1 |
| 空列表 | [], ε=1.0 | 0 + noise | P0 |
| 自定义灵敏度 | value, Δf=2.0, ε=1.0 | noise scale=2.0 | P1 |
| 线程安全 | 多线程并发 | 无竞态 | P1 |
| 统计分布 | 10000次查询 | 噪声均值≈0 | P2 |

## 3. 统计验证

```java
@Test
void testNoiseDistributionMeanIsZero() {
    DpApi dp = new DpApi();
    double sum = 0;
    int n = 10000;
    for (int i = 0; i < n; i++) {
        sum += dp.addLaplaceNoise(0.0, 1.0, 1.0);
    }
    double mean = sum / n;
    // 噪声均值应接近 0
    assertTrue(Math.abs(mean) < 0.1);
}
```

## 4. 运行测试

```bash
mvn test -pl agent/agent-sdk -Dtest=DpApiTest
mvn test -pl agent/agent-server -Dtest=DpControllerIntegrationTest
```
