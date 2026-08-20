# 医疗流水线 — 测试文档

## 1. 测试策略

### 单元测试

```java
@Test
void testMedicalPipelineProcess() {
    MedicalPipeline pipeline = new MedicalPipeline();
    MedicalRecord record = MedicalRecord.builder()
        .patientName("张三")
        .phone("13812345678")
        .diagnosis("糖尿病")
        .build();

    PipelineResult result = pipeline.process(record);

    // 验证脱敏
    assertNotEquals("张三", result.getMaskedRecord().getPatientName());
    assertNotEquals("13812345678", result.getMaskedRecord().getPhone());

    // 验证分类
    assertFalse(result.getClassifications().isEmpty());

    // 验证审计
    assertNotNull(result.getAudit());
}

@Test
void testMedicalPipelineBatch() {
    MedicalPipeline pipeline = new MedicalPipeline();
    List<MedicalRecord> records = List.of(
        MedicalRecord.builder().patientName("张三").build(),
        MedicalRecord.builder().patientName("李四").build()
    );
    List<PipelineResult> results = pipeline.processBatch(records);
    assertEquals(2, results.size());
}
```

## 2. 测试用例矩阵

| 场景 | 输入 | 预期 | 优先级 |
|---|---|---|---|
| 基本处理 | 含姓名+手机 | 脱敏+分类 | P0 |
| 批量处理 | 3条记录 | 3条结果 | P0 |
| 空记录 | null | 抛异常 | P0 |
| 全字段 | 完整医疗记录 | 全部脱敏 | P0 |
| 审计记录 | 任意输入 | 审计非空 | P1 |
| 超时处理 | 慢 LLM | 超时异常 | P1 |

## 3. 运行测试

```bash
mvn test -pl agent/agent-sdk -Dtest=MedicalPipelineTest
mvn test -pl agent/agent-server -Dtest=MedicalControllerIntegrationTest
```
