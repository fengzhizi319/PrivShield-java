# 高并发 — 使用示例

## 并发测试

```java
ExecutorService executor = Executors.newFixedThreadPool(100);
List<CompletableFuture<String>> futures = new ArrayList<>();
MaskingApi api = new MaskingApi();

for (int i = 0; i < 10000; i++) {
    futures.add(CompletableFuture.supplyAsync(
        () -> api.maskValue("mobile", "13812345678"),
        executor
    ));
}

CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
```
