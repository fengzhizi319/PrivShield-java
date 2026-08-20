# 部署 — 测试文档

## 1. 部署验证清单

- [ ] Agent REST 健康检查通过
- [ ] Agent gRPC 健康检查通过
- [ ] Go Backend 健康检查通过
- [ ] 前端页面可访问
- [ ] 端到端功能验证通过 (Mask/DP/KAnon)
- [ ] TLS 证书有效
- [ ] 日志正常输出
- [ ] 监控指标可采集

## 2. 冒烟测试

```bash
#!/bin/bash
set -e

echo "=== 冒烟测试 ==="

# 1. 健康检查
curl -sf http://localhost:8079/actuator/health || { echo "Agent health failed"; exit 1; }
curl -sf http://localhost:8081/health || { echo "Backend health failed"; exit 1; }

# 2. 功能验证
RESULT=$(curl -sf -X POST http://localhost:8079/v1/privacy/mask \
  -H "Content-Type: application/json" \
  -d '{"value": "13812345678", "field_name": "mobile"}')
echo "$RESULT" | grep -q "138\*\*\*\*5678" || { echo "Mask failed"; exit 1; }

echo "=== 冒烟测试通过 ==="
```

## 3. 性能测试

```bash
# Agent 压力测试
ab -n 10000 -c 100 http://localhost:8079/v1/health

# gRPC 压力测试
ghz --insecure --proto privacy.proto \
  --call privacy.PrivacyService/Mask \
  -d '{"field_name":"mobile","value":"13812345678"}' \
  -n 10000 -c 100 localhost:50051
```
