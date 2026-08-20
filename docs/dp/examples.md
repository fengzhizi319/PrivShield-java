# 差分隐私使用示例

## 1. Java SDK 示例

### 1.1 DP Count

```java
DpApi dp = new DpApi();
List<Integer> values = List.of(1, 2, 3, 4, 5);
double result = dp.dpCount(values, 1.0);
// result ≈ 5.0 + noise (e.g., 4.5)
```

### 1.2 DP Sum

```java
List<Double> values = List.of(10.0, 20.0, 30.0);
double result = dp.dpSum(values, 0.5);
// result ≈ 60.0 + noise
```

### 1.3 DP Mean

```java
List<Double> values = List.of(10.0, 20.0, 30.0, 40.0, 50.0);
double result = dp.dpMean(values, 1.0);
// result ≈ 30.0 + noise
```

### 1.4 自定义灵敏度

```java
double result = dp.addLaplaceNoise(100.0, 1.0, 0.5);
// sensitivity=1.0, epsilon=0.5 → noise scale = 2.0
```

## 2. REST API 示例

```bash
# DP Count
curl -X POST http://localhost:8079/v1/privacy/dp/count \
  -H "Content-Type: application/json" \
  -d '{"values": [1,2,3,4,5], "epsilon": 1.0}'
# {"result": 4.5}

# DP Sum
curl -X POST http://localhost:8079/v1/privacy/dp/sum \
  -H "Content-Type: application/json" \
  -d '{"values": [10,20,30], "epsilon": 0.5}'
# {"result": 58.7}

# DP Mean
curl -X POST http://localhost:8079/v1/privacy/dp/mean \
  -H "Content-Type: application/json" \
  -d '{"values": [10,20,30,40,50], "epsilon": 1.0}'
# {"result": 31.2}
```

## 3. 不同 ε 值对比

```java
DpApi dp = new DpApi();
List<Integer> data = IntStream.range(1, 101).boxed().collect(Collectors.toList());

// ε=10.0 — 强隐私，噪声大
System.out.println(dp.dpCount(data, 10.0));  // 可能偏离较多

// ε=1.0 — 平衡
System.out.println(dp.dpCount(data, 1.0));   // 接近真实值

// ε=0.1 — 弱隐私，噪声小
System.out.println(dp.dpCount(data, 0.1));   // 非常接近真实值
```

## 4. 最佳实践

1. **ε 选择**: 医疗数据推荐 ε ∈ [0.1, 1.0]，统计查询推荐 ε ∈ [1.0, 10.0]
2. **数据范围**: Sum 操作建议提供数据范围 [a, b] 以精确计算灵敏度
3. **预算跟踪**: 使用 BudgetAccountant 跟踪累计 ε 消耗
4. **多次查询**: 组合查询时注意 ε 的累积消耗（组合定理）
