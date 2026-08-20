# 数据脱敏模块 — 测试文档

## 1. 测试策略

### 单元测试
覆盖所有脱敏策略和字段类型识别逻辑。

```java
@Test
void testMaskMobile() {
    MaskingApi api = new MaskingApi();
    assertEquals("138****5678", api.maskValue("mobile", "13812345678"));
}

@Test
void testMaskName() {
    MaskingApi api = new MaskingApi();
    assertEquals("张**丰", api.maskValue("name", "张三丰"));
}

@Test
void testMaskIdCard() {
    MaskingApi api = new MaskingApi();
    assertEquals("110101********1234", api.maskValue("id_card", "110101199001011234"));
}
```

### 集成测试
验证 REST 和 gRPC 端点。

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MaskControllerIntegrationTest {
    @Test
    void testMaskEndpoint() {
        // POST /v1/privacy/mask → 200 + masked result
    }
}
```

## 2. 测试用例矩阵

| 场景 | 输入 | 预期输出 | 优先级 |
|---|---|---|---|
| 手机号脱敏 | 13812345678 | 138****5678 | P0 |
| 姓名脱敏 | 张三丰 | 张**丰 | P0 |
| 身份证脱敏 | 110101199001011234 | 110101********1234 | P0 |
| 邮箱脱敏 | test@example.com | t***t@example.com | P0 |
| 空值处理 | null / "" | 原样返回 | P0 |
| 未知字段 | random_value | 通用掩码 | P1 |
| 批量脱敏 | 多字段 | 各自脱敏 | P1 |
| HMAC 哈希 | value + salt | 确定性哈希 | P1 |
| 并发安全 | 多线程同时调用 | 无竞态 | P1 |

## 3. 性能基准

```bash
# 运行基准测试
mvn test -pl agent/agent-sdk -Dtest=MaskingBenchmark

# 预期结果
# 单字段脱敏: < 1ms (P99)
# 批量 1000 条: < 50ms
```

## 4. 运行测试

```bash
# 单元测试
mvn test -pl agent/agent-sdk -Dtest=MaskingApiTest

# 集成测试
mvn test -pl agent/agent-server -Dtest=MaskControllerIntegrationTest

# 全部测试
mvn test
```
