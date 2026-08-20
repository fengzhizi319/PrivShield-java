# 隐私处理流水线 — 设计文档

## 1. 概述

隐私处理流水线 (Privacy Pipeline) 提供可组合的隐私处理链，支持将多个隐私原语串联为端到端处理流程。

## 2. 架构

```
Input → [Stage1: Classify] → [Stage2: Mask] → [Stage3: DP] → [Stage4: Audit] → Output
```

## 3. Stage 接口

```java
public interface PipelineStage<I, O> {
    O process(I input, PipelineContext context);
    String name();
    int order();
}
```

## 4. 内置 Stage

| Stage | 输入 | 输出 | 说明 |
|---|---|---|---|
| ClassifyStage | Field | ClassifiedField | 分类分级 |
| MaskStage | ClassifiedField | MaskedField | 脱敏处理 |
| DpStage | Aggregation | NoisyResult | 差分隐私 |
| AuditStage | PipelineResult | AuditRecord | 审计记录 |
