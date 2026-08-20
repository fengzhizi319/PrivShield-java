# 安全加固 — 设计文档

## 1. TLS/mTLS

| 模式 | 说明 | 适用场景 |
|---|---|---|
| TLS | 单向加密 | 内部网络 |
| mTLS | 双向证书认证 | 跨域/零信任 |

## 2. 认证机制

### API Key
```
Authorization: Bearer <api-key>
```

### JWT
```
Authorization: Bearer <jwt-token>
```

## 3. 审计日志

| 字段 | 说明 |
|---|---|
| timestamp | 操作时间 |
| principal | 操作者身份 |
| action | 操作类型 |
| resource | 操作资源 |
| result | 操作结果 |
| ip | 来源 IP |
