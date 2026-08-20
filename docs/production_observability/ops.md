# 可观测性 — 运维手册

## Prometheus 配置

```yaml
scrape_configs:
  - job_name: 'privshield-java'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['agent:8079']
    scrape_interval: 15s
```

## 日志轮转

```yaml
# logback-spring.xml
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
  <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <fileNamePattern>/logs/agent.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
    <maxFileSize>100MB</maxFileSize>
    <maxHistory>30</maxHistory>
  </rollingPolicy>
</appender>
```
