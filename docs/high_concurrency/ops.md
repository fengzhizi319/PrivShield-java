# 高并发 — 运维手册

## JVM 调优

```bash
java -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+ParallelRefProcEnabled \
  -jar agent-server.jar
```

## 线程池配置

| 参数 | 默认值 | 说明 |
|---|---|---|
| server.tomcat.threads.max | 200 | 最大工作线程 |
| server.tomcat.accept-count | 100 | 等待队列长度 |
| grpc.server.thread-count | 100 | gRPC 线程数 |
