# 查询混淆 (Query Obfuscation / QoL)

> 对 SQL 查询进行混淆处理，注入噪声记录，防止查询结果泄露敏感信息。

## 功能概述

- **SQL 查询混淆**: 生成语义相似但参数不同的噪声查询
- **噪声记录注入**: 在查询结果中混入伪造记录
- **列级混淆**: 对指定列的值进行替换

## 快速开始

```bash
curl -X POST http://localhost:8079/v1/privacy/qol/obfuscate \
  -H "Content-Type: application/json" \
  -d '{
    "query": "SELECT * FROM patients WHERE name=\"张三\"",
    "columns": ["name"]
  }'
# 响应: 原始查询 + 3 条噪声查询
```

## 文档索引

| 文档 | 说明 |
|------|------|
| [设计文档](design.md) | 混淆策略设计 |
| [API 参考](api_reference.md) | REST + gRPC 接口 |
| [使用示例](examples.md) | 场景示例 |
| [运维指南](ops.md) | 配置说明 |
| [测试指南](testing.md) | 测试方法 |
| [产品需求](prd.md) | 功能规格 |
