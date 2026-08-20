# 安全加固 — 使用示例

## mTLS 配置

```yaml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_PASSWORD}
    trust-store: classpath:truststore.p12
    trust-store-password: ${SSL_TRUST_PASSWORD}
    client-auth: need
```

## API Key 认证

```bash
curl -H "Authorization: Bearer sk-xxxxx" \
  http://localhost:8079/v1/privacy/mask \
  -d '{"value": "13812345678", "field_name": "mobile"}'
```
