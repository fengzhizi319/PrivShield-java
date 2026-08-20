# 查询混淆 — 设计文档

## 1. 混淆策略

### 查询级混淆

1. 解析原始 SQL 查询
2. 替换 WHERE 条件中的敏感值
3. 生成 N 条噪声查询
4. 并行执行所有查询
5. 合并结果集

### 列级混淆

对指定列的值进行替换：
- 数值列: 添加 Laplace 噪声
- 文本列: 从同分布采样替换
- 日期列: 随机偏移

## 2. 类设计

```
QolApi
├── obfuscate(query, columns) → List<String>
├── obfuscateResult(result, columns) → List<Map>
└── addNoise(value, columnType) → Object
```
