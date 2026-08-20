# 高并发支持

> PrivShield Java 的高并发处理能力设计与实现。

## 核心设计

- **线程安全**: ThreadLocalRandom 替代 Random
- **无锁设计**: 无状态 API，天然线程安全
- **连接池**: gRPC Channel 复用
- **异步处理**: CompletableFuture 链式调用

## 性能指标

| 场景 | QPS | P99 延迟 |
|---|---|---|
| Mask 单字段 | > 50,000 | < 1ms |
| DP Count | > 30,000 | < 0.5ms |
| K-Anonymity (100条) | > 1,000 | < 100ms |
