# 高并发 — 设计文档

## 1. 线程安全策略

| 组件 | 策略 | 说明 |
|---|---|---|
| MaskingApi | 无状态 | 无共享可变状态 |
| DpApi | ThreadLocalRandom | 随机数线程隔离 |
| KAnonymityApi | 无状态 | 纯函数式 |
| QolApi | ThreadLocalRandom | 随机数线程隔离 |

## 2. 连接管理

- gRPC: ManagedChannel 连接池
- REST: Spring Boot 内嵌 Tomcat 线程池
- 数据库: HikariCP 连接池

## 3. 背压控制

- 请求队列 + 有界缓冲
- 超时控制
- 降级策略
