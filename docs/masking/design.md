# 数据脱敏 — 设计文档

## 1. 设计目标

- 零配置即可对常见敏感字段进行有效脱敏
- 支持自定义脱敏策略，通过 YAML 规则配置
- 脱敏结果保持原始数据格式，不影响下游系统

## 2. 核心算法

### 2.1 字段类型识别

```
输入: (value, field_name)
  │
  ├── field_name 匹配规则? ──→ 使用对应策略
  │
  └── 正则模式匹配? ──→ 自动检测类型
        ├── 手机号模式: 1[3-9]\d{9}
        ├── 身份证模式: \d{17}[\dXx]
        ├── 邮箱模式: \S+@\S+
        └── 默认: 通用掩码
```

### 2.2 掩码策略

| 策略 | 实现 | 适用场景 |
|------|------|----------|
| 保留首尾 | `first + "***" + last` | 姓名 |
| 保留前N后M | `prefix + "***" + suffix` | 手机/身份证/银行卡 |
| 哈希替换 | `hash(value)[:len]` | 需要一致性脱敏 |
| 完全遮蔽 | `"***"` | 高敏字段 |

## 3. 类设计

```
MaskingApi
├── mask(value, fieldName) → String
├── maskRecord(record, fieldNames) → Map<String, String>
└── maskTable(rows, fieldNames) → List<Map<String, String>>
```

## 4. 性能考量

- 单次 mask 操作 < 0.1ms
- 批量 maskTable 支持并行处理
- 正则编译缓存，避免重复编译
