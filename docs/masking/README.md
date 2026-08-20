# 数据脱敏 (Masking)

> 智能识别并掩码敏感信息，支持姓名、手机号、身份证号、邮箱、银行卡号等多种字段类型。

## 功能概述

- **智能字段识别**: 根据字段名自动选择脱敏策略
- **多类型支持**: 姓名/手机/身份证/邮箱/银行卡/地址
- **可配置策略**: 通过 YAML 规则自定义掩码规则
- **保留格式**: 脱敏后保持原始数据格式

## 快速开始

### REST API

```bash
curl -X POST http://localhost:8079/v1/privacy/mask \
  -H "Content-Type: application/json" \
  -d '{"value": "张三 13812345678", "field_name": "name"}'
```

### gRPC

```protobuf
rpc Mask(MaskRequest) returns (MaskResponse);
```

### Java SDK

```java
PrivacyClient client = PrivacyClient.builder().build();
String result = client.mask("张三 13812345678", "name");
// result: "张**8"
```

## 脱敏规则

| 类型 | 原始值 | 脱敏结果 | 规则 |
|------|--------|----------|------|
| 姓名 | 张三 | 张* | 保留姓，名用 * |
| 手机 | 13812345678 | 138****5678 | 保留前3后4 |
| 身份证 | 110101199001011234 | 110101********1234 | 保留前6后4 |
| 邮箱 | test@example.com | t***@example.com | 保留首字母 |
| 银行卡 | 6222021234567890 | 6222********7890 | 保留前4后4 |

## 文档索引

| 文档 | 说明 |
|------|------|
| [设计文档](design.md) | 脱敏算法设计与策略 |
| [API 参考](api_reference.md) | REST + gRPC 接口定义 |
| [使用示例](examples.md) | 常见场景示例 |
| [运维指南](ops.md) | 配置与调优 |
| [测试指南](testing.md) | 测试方法 |
| [产品需求](prd.md) | 功能需求规格 |
