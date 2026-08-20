# 高并发设计

> PrivShield Java 的高并发架构设计与性能优化。

## 设计原则

1. **无状态优先**: 所有隐私 API 设计为无状态，天然线程安全
2. **线程隔离**: 使用 ThreadLocalRandom 避免锁竞争
3. **连接复用**: gRPC Channel 和 HTTP 连接池

## 性能数据

| 模块 | 单线程 QPS | 100 线程 QPS | P99 延迟 |
|---|---|---|---|
| Mask 单字段 | 500K | 2M+ | < 0.5ms |
| DP Count (100) | 300K | 1M+ | < 0.3ms |
| K-Anonymity (100) | 10K | 50K | < 50ms |
| QoL Query | 100K | 500K | < 2ms |

## 关键实现

### ThreadLocalRandom

```java
// 线程安全的随机数生成
private static double laplaceNoise(double scale) {
    double u = ThreadLocalRandom.current().nextDouble() - 0.5;
    return -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
}
```

### 无状态 MaskingApi

```java
// 无共享可变状态，可安全并发使用
public class MaskingApi {
    public String maskValue(String fieldName, String value) {
        // 纯函数：相同输入 → 相同输出
        return strategy.mask(value);
    }
}
```

## 压测方法

```bash
# REST 压测
wrk -t12 -c400 -d30s http://localhost:8079/v1/health

# gRPC 压测
ghz --insecure --proto privacy.proto \
  --call privacy.PrivacyService/Mask \
  -d '{"field_name":"mobile","value":"13812345678"}' \
  -n 100000 -c 200 localhost:50051
```
