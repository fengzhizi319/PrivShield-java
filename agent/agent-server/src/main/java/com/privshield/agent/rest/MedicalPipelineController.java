package com.privshield.agent.rest;

import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 医疗数据处理 + 通用流水线 REST 端点 — 对应 Python routers/medical.py 和 pipeline/。
 */
@RestController
public class MedicalPipelineController {

    private final PrivacyService service;

    public MedicalPipelineController(PrivacyService service) {
        this.service = service;
    }

    // ─── 医疗流水线 /v1/medical/process ───

    @SuppressWarnings("unchecked")
    @PostMapping("/v1/medical/process")
    public ResponseEntity<Map<String, Object>> processMedical(@RequestBody Map<String, Object> body) {
        List<Map<String, String>> records = new ArrayList<>();
        Object recsObj = body.get("records");
        if (recsObj instanceof List) {
            for (Object r : (List<?>) recsObj) {
                if (r instanceof Map) {
                    Map<String, String> rec = new LinkedHashMap<>();
                    ((Map<String, Object>) r).forEach((k, v) -> rec.put(k, v != null ? v.toString() : ""));
                    records.add(rec);
                }
            }
        }

        // 分类分级 + L4/L5 强抹平脱敏
        List<Map<String, Object>> classificationReport = new ArrayList<>();
        List<Map<String, String>> sanitizedData = new ArrayList<>();

        for (Map<String, String> record : records) {
            // 分类
            var classResult = service.classifyRecord(record, "medical", null);
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("max_level", classResult != null && classResult.getFinalLevel() != null ? classResult.getFinalLevel() : "");
            classificationReport.add(report);

            // 脱敏：对每个字段执行 mask
            Map<String, String> sanitized = service.maskRecord(record, "");
            sanitizedData.add(sanitized);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total_records", records.size());
        summary.put("processed_at", java.time.Instant.now().toString());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("classification_report", classificationReport);
        response.put("sanitized_data", sanitizedData);
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

    // ─── 通用流水线 /v1/pipeline/process_records ───

    @SuppressWarnings("unchecked")
    @PostMapping("/v1/pipeline/process_records")
    public ResponseEntity<Map<String, Object>> processRecords(@RequestBody Map<String, Object> body) {
        List<Map<String, String>> records = new ArrayList<>();
        Object recsObj = body.get("records");
        if (recsObj instanceof List) {
            for (Object r : (List<?>) recsObj) {
                if (r instanceof Map) {
                    Map<String, String> rec = new LinkedHashMap<>();
                    ((Map<String, Object>) r).forEach((k, v) -> rec.put(k, v != null ? v.toString() : ""));
                    records.add(rec);
                }
            }
        }

        boolean classify = body.containsKey("classify") ? (Boolean) body.get("classify") : true;
        boolean maskL4 = body.containsKey("mask_l4") ? (Boolean) body.get("mask_l4") : true;
        boolean maskL5 = body.containsKey("mask_l5") ? (Boolean) body.get("mask_l5") : true;

        List<Map<String, String>> processedRecords = new ArrayList<>();
        for (Map<String, String> record : records) {
            Map<String, String> processed = new LinkedHashMap<>(record);
            if (classify) {
                service.classifyRecord(record, null, null);
            }
            if (maskL4 || maskL5) {
                processed = service.maskRecord(record, "");
            }
            processedRecords.add(processed);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("records", processedRecords);
        response.put("total", processedRecords.size());
        return ResponseEntity.ok(response);
    }

    // ─── 运维诊断 /v1/ops/diagnostics ───

    @GetMapping("/v1/ops/diagnostics")
    public ResponseEntity<Map<String, Object>> diagnostics() {
        Map<String, Object> diag = new LinkedHashMap<>();
        diag.put("status", "ok");
        diag.put("engine", "java-agent");
        diag.put("timestamp", java.time.Instant.now().toString());
        diag.put("budget_remaining", service.budgetRemaining());
        return ResponseEntity.ok(diag);
    }

    // ─── Profile 管理 ───

    @GetMapping("/v1/privacy/profile")
    public ResponseEntity<Map<String, Object>> getProfile() {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("namespace", "default");
        profile.put("status", "active");
        return ResponseEntity.ok(profile);
    }

    // ─── 文件处理 ───

    @PostMapping("/v1/privacy/file/mask")
    public ResponseEntity<Map<String, Object>> fileMask(@RequestBody Map<String, Object> body) {
        // 简化实现：接受 records 格式
        @SuppressWarnings("unchecked")
        List<Map<String, String>> data = (List<Map<String, String>>) body.getOrDefault("data", List.of());
        List<Map<String, String>> result = service.maskDataFrame(data, null, "");
        return ResponseEntity.ok(Map.of("result", result));
    }
}
