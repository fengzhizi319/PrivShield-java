# K-匿名化 (K-Anonymity)

> 通过准标识符泛化确保每条记录在数据集中至少有 k-1 条不可区分记录。

## 功能概述

- **记录级泛化**: 单条记录的准标识符泛化
- **表级匿名化**: 整表的 K-匿名化处理
- **泛化层次**: 支持自定义泛化层次结构
- **自适应 K**: 根据数据分布自动选择泛化层级

## 快速开始

```bash
curl -X POST http://localhost:8079/v1/privacy/k_anonymize/record \
  -H "Content-Type: application/json" \
  -d '{
    "record": {"name": "张三", "age": "30", "city": "北京"},
    "qi_cols": ["name", "age"],
    "k": 2
  }'
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [设计文档](design.md) | Mondrian 分割与泛化策略 |
| [API 参考](api_reference.md) | REST + gRPC 接口 |
| [使用示例](examples.md) | 场景示例 |
| [运维指南](ops.md) | 配置与调优 |
| [测试指南](testing.md) | 验证方法 |
| [产品需求](prd.md) | 功能规格 |
