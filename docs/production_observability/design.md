# 可观测性 — 设计文档

## 1. 日志规范

| 字段 | 类型 | 说明 |
|---|---|---|
| timestamp | ISO8601 | 时间戳 |
| level | String | 日志级别 |
| logger | String | Logger 名称 |
| message | String | 日志消息 |
| traceId | String | 追踪 ID |
| spanId | String | 跨度 ID |
| module | String | 模块名 |

## 2. 指标体系

### 隐私操作指标
- `privacy_*_operations_total` — 操作计数
- `privacy_*_duration_seconds` — 操作延迟
- `privacy_*_errors_total` — 错误计数

### 系统指标
- JVM 内存/GC
- 线程池利用率
- gRPC 连接数

## 3. 追踪集成

```java
// OpenTelemetry 自动注入
@WithSpan
public String maskValue(String fieldName, String value) {
    Span.current().setAttribute("field.name", fieldName);
    // ...
}
```
