# 差分隐私 (Differential Privacy)

> 基于 Laplace 机制的差分隐私查询，支持 Count/Sum/Mean 三种聚合操作，通过 ε 参数控制隐私-精度权衡。

## 功能概述

- **ε (epsilon) 隐私预算**: 控制噪声强度，ε 越小隐私越强
- **Laplace 噪声注入**: 满足 (ε, 0)-差分隐私
- **三种聚合**: Count / Sum / Mean
- **灵敏度自动计算**: 根据数据类型自动确定全局灵敏度

## 快速开始

```bash
# DP Count
curl -X POST http://localhost:8079/v1/privacy/dp/count \
  -H "Content-Type: application/json" \
  -d '{"values": [1,2,3,4,5], "epsilon": 1.0}'
# 响应: {"result": 4.5}  (真实=5)

# DP Sum
curl -X POST http://localhost:8079/v1/privacy/dp/sum \
  -H "Content-Type: application/json" \
  -d '{"values": [10,20,30], "epsilon": 0.5}'

# DP Mean
curl -X POST http://localhost:8079/v1/privacy/dp/mean \
  -H "Content-Type: application/json" \
  -d '{"values": [10,20,30,40,50], "epsilon": 1.0}'
```

## 隐私保证

| ε 值 | 隐私强度 | 噪声量 | 适用场景 |
|------|----------|--------|----------|
| 0.1 | 强 | 大 | 高隐私需求 |
| 1.0 | 中 | 中 | 通用场景 |
| 10.0 | 弱 | 小 | 精度优先 |

## 文档索引

| 文档 | 说明 |
|------|------|
| [设计文档](design.md) | Laplace 机制与灵敏度分析 |
| [API 参考](api_reference.md) | REST + gRPC 接口 |
| [使用示例](examples.md) | ε 选择与场景示例 |
| [运维指南](ops.md) | 预算管理与监控 |
| [测试指南](testing.md) | 统计验证方法 |
| [产品需求](prd.md) | 功能规格 |
