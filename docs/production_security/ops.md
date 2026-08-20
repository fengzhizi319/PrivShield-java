# 安全加固 — 运维手册

## 证书管理

```bash
# 证书轮换
kubectl create secret tls agent-tls --cert=new-cert.pem --key=new-key.pem
kubectl rollout restart deployment/agent
```

## 安全扫描

```bash
# 依赖漏洞扫描
mvn dependency-check:check

# 静态分析
mvn spotbugs:check
```
