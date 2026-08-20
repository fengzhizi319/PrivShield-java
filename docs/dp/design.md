# 差分隐私 — 设计文档

## 1. 数学基础

### Laplace 机制

对于函数 f: D → R，全局灵敏度 Δf = max|f(D) - f(D')|，
Laplace 机制发布 f(D) + Lap(Δf/ε) 满足 (ε, 0)-差分隐私。

### 灵敏度分析

| 聚合 | 灵敏度 Δf | 说明 |
|------|-----------|------|
| Count | 1 | 增删一条记录最多改变计数 1 |
| Sum (范围 [a,b]) | b - a | 增删一条记录最多改变求和 b-a |
| Mean (范围 [a,b], n 条) | (b-a)/n | 随数据量增大灵敏度降低 |

## 2. 实现架构

```
DpApi
├── dpCount(values, epsilon) → double
├── dpSum(values, epsilon) → double
├── dpMean(values, epsilon) → double
└── addLaplaceNoise(value, sensitivity, epsilon) → double
```

## 3. 线程安全

使用 `ThreadLocalRandom` 生成均匀随机数，通过逆变换采样生成 Laplace 分布：
- U ~ Uniform(-0.5, 0.5)
- Laplace = -sign(U) * (sensitivity/ε) * ln(1 - 2|U|)
