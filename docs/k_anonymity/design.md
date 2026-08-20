# K-匿名化 — 设计文档

## 1. 算法选择

### Mondrian 分割

采用 top-down 分割策略：
1. 选择分裂维度（准标识符列）
2. 按中位数分割
3. 递归直到每个分区大小 ≤ k

### 泛化层次

| 列类型 | Level 0 | Level 1 | Level 2 |
|--------|---------|---------|---------|
| 年龄 | 精确值 | 年龄段 (20-30) | 粗粒度 (青年) |
| 姓名 | 全名 | 姓+* | * |
| 地址 | 完整地址 | 区级 | 市级 |

## 2. 类设计

```
KAnonymityApi
├── anonymizeRecord(record, qiCols, hierarchies, k) → Map
├── anonymizeTable(rows, qiCols, hierarchies, k) → List<Map>
└── chooseLevel(k, hierarchy) → int
```
