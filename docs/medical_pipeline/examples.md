# 医疗流水线使用示例

## 1. Java SDK 示例

### 1.1 单条记录处理

```java
MedicalPipeline pipeline = new MedicalPipeline();

MedicalRecord record = MedicalRecord.builder()
    .patientName("张三")
    .phone("13812345678")
    .idCard("110101199001011234")
    .diagnosis("2型糖尿病")
    .prescription("二甲双胍 500mg bid")
    .build();

PipelineResult result = pipeline.process(record);

// 脱敏结果
System.out.println(result.getMaskedRecord().getPatientName()); // 张*
System.out.println(result.getMaskedRecord().getPhone());       // 138****5678

// 分类结果
for (FieldClassification fc : result.getClassifications()) {
    System.out.printf("%s → %s (%.2f)%n", fc.getField(), fc.getLevel(), fc.getConfidence());
}
```

### 1.2 批量处理

```java
List<MedicalRecord> records = List.of(record1, record2, record3);
List<PipelineResult> results = pipeline.processBatch(records);
```

## 2. REST API 示例

```bash
# 单条处理
curl -X POST http://localhost:8079/v1/privacy/medical/process \
  -H "Content-Type: application/json" \
  -d '{
    "patient_name": "张三",
    "phone": "13812345678",
    "diagnosis": "2型糖尿病"
  }'

# 批量处理
curl -X POST http://localhost:8079/v1/privacy/medical/process/batch \
  -H "Content-Type: application/json" \
  -d '{"records": [
    {"patient_name": "张三", "phone": "13812345678"},
    {"patient_name": "李四", "phone": "13912345678"}
  ]}'
```

## 3. 流水线处理流程

```
原始记录 → 字段分类 → 敏感度判定 → 脱敏策略选择 → 执行脱敏 → 审计记录 → 输出
```
