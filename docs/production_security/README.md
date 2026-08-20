# 生产安全加固

> PrivShield Java 的安全加固措施，覆盖传输安全、认证授权、审计合规。

## 安全架构

```
Client → TLS/mTLS → Agent
         ↓
    认证 (API Key / JWT)
         ↓
    授权 (RBAC / Whitelist)
         ↓
    审计日志
```

## 核心能力

- **TLS/mTLS**: 传输层加密 + 双向认证
- **API Key 认证**: 简单场景
- **JWT 认证**: 复杂场景
- **IP 白名单**: 网络层访问控制
- **审计日志**: 全操作审计

## 快速开始

```bash
# 生成自签名证书
openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 -nodes

# 配置 TLS
java -Dserver.ssl.enabled=true \
     -Dserver.ssl.key-store=keystore.p12 \
     -Dserver.ssl.key-store-password=changeit \
     -jar agent-server.jar
```
