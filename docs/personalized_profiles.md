# 个性化隐私配置 (Personalized Profiles)

> 支持为不同业务场景、不同数据类型定义个性化的隐私处理配置 Profile。

## 概述

PrivShield Java 通过 YAML 配置文件定义隐私 Profile，每个 Profile 包含：
- 分类分级规则集
- 脱敏策略映射
- 差分隐私参数
- K-匿名化参数

## 配置示例

```yaml
# config/profiles/medical.yaml
name: medical
description: 医疗健康数据处理配置

classification:
  rules:
    - pattern: "patient_name|name"
      level: L3
      category: "个人身份信息"
    - pattern: "diagnosis|诊断"
      level: L4
      category: "健康医疗信息"
    - pattern: "gene|基因"
      level: L5
      category: "生物特征信息"

masking:
  strategies:
    L3: "partial_mask"
    L4: "heavy_mask"
    L5: "remove"

dp:
  default_epsilon: 1.0
  max_epsilon: 10.0

k_anonymity:
  default_k: 5
  quasi_identifiers: ["age", "gender", "city"]
```

## Profile 加载

```java
// 加载默认 profile
PrivacyProfile profile = PrivacyProfile.load("medical");

// 使用 profile 进行分类
ClassificationResult result = engine.classifyWithProfile("name", "张三", "medical");
```

## 内置 Profile

| Profile | 适用场景 | 默认 K | 默认 ε |
|---|---|---|---|
| medical | 医疗健康 | 5 | 1.0 |
| finance | 金融 | 3 | 0.5 |
| government | 政务 | 10 | 0.1 |
| general | 通用 | 5 | 1.0 |
