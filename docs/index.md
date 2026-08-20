# PrivShield Java 文档中心

> 企业级数据隐私计算 Java Agent 完整文档索引。

---

## 架构与设计

| 文档 | 说明 |
|------|------|
| [架构设计](architecture-design.md) | 系统整体架构、模块划分、数据流 |
| [架构概览](architecture-summary.md) | 高层架构摘要与技术选型 |
| [高并发设计](high_concurr.md) | 高并发架构设计与性能数据 |
| [生产改进](production_improvements.md) | 生产环境持续改进记录 |
| [个性化配置](personalized_profiles.md) | YAML 驱动的隐私 Profile |

## 处理原语 (Privacy Primitives)

| 模块 | 文档目录 | 说明 |
|------|----------|------|
| 数据脱敏 (Masking) | [masking/](masking/) | 姓名/手机/身份证智能掩码 |
| 差分隐私 (DP) | [dp/](dp/) | Count/Sum/Mean 加噪查询 |
| K-匿名化 (K-Anonymity) | [k_anonymity/](k_anonymity/) | 记录级准标识符泛化 |
| 查询混淆 (QoL) | [qol/](qol/) | SQL 查询混淆 + 噪声注入 |

## 高级功能

| 模块 | 文档目录 | 说明 |
|------|----------|------|
| 动态分类分级 | [dynclassification/](dynclassification/) | 三层漏斗: 规则→NER→LLM |
| 医疗流水线 | [medical_pipeline/](medical_pipeline/) | 分类 + 脱敏一体化处理 |
| 隐私流水线 | [pipeline/](pipeline/) | 可组合隐私处理链 |

## 基础设施

| 模块 | 文档目录 | 说明 |
|------|----------|------|
| 部署 | [deployment/](deployment/) | 本地/Docker/K8s 部署方案 |
| 网关负载均衡 | [gateway_balancer/](gateway_balancer/) | gRPC+REST 双协议网关 |
| 高并发 | [high_concurrency/](high_concurrency/) | 线程安全与性能优化 |
| 可观测性 | [production_observability/](production_observability/) | 日志/指标/追踪 |
| 安全加固 | [production_security/](production_security/) | TLS/mTLS/认证/审计 |

## 标准与合规

| 文档 | 说明 |
|------|------|
| [标准规范](standard/) | 国标/行标/地方标准 |
| [审计报告](audit_reports/) | 项目审计报告 |
| [产品洞察](主流数据安全产品洞察报告.md) | 主流数据安全产品对比 |
| [技术分析](分析报告.md) | 隐私保护技术分类分析 |

## 各模块文档结构

每个功能模块包含以下标准文档集：

| 文档 | 说明 |
|------|------|
| `README.md` | 功能概述、快速开始 |
| `design.md` | 架构设计、算法说明 |
| `api_reference.md` | Java SDK / REST / gRPC API |
| `examples.md` | 使用示例、最佳实践 |
| `ops.md` | 运维手册、监控告警 |
| `prd.md` | 产品需求文档 |
| `testing.md` | 测试策略、用例矩阵 |
