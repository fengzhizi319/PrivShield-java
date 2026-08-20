# 高并发 — 测试文档

## 并发测试

```bash
# JMeter 或 wrk 压测
wrk -t12 -c400 -d30s http://localhost:8079/v1/health
```

## 竞态检测

```bash
# JCStress 竞态测试
mvn test -pl agent/agent-sdk -Dtest=ConcurrencyTest
```
