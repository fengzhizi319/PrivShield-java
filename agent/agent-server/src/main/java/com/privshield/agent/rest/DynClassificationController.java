package com.privshield.agent.rest;

import com.github.fengzhizi319.privacy.sdk.dynclassification.model.FieldClassificationResult;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.RecordClassificationResult;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.SecurityTag;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.TableClassificationResult;
import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态分类分级 REST 端点 — 对应 Python routers/dynclassification.py。
 */
@RestController
@RequestMapping("/v1/dynclassification")
public class DynClassificationController {

    private final PrivacyService service;

    public DynClassificationController(PrivacyService service) {
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/eval")
    public ResponseEntity<Map<String, Object>> evaluateField(@RequestBody Map<String, Object> body) {
        String fieldName = (String) body.getOrDefault("field_name",
                body.getOrDefault("fieldName", ""));
        String value = body.get("value") != null ? body.get("value").toString() : "";
        String domain = (String) body.get("domain");
        String standard = (String) body.get("standard");

        FieldClassificationResult result = service.classifyField(fieldName, value, domain, standard);
        return ResponseEntity.ok(buildFieldResponse(result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/eval_record")
    public ResponseEntity<Map<String, Object>> evalRecord(@RequestBody Map<String, Object> body) {
        Map<String, String> record = new LinkedHashMap<>();
        Object recObj = body.get("record");
        if (recObj instanceof Map) {
            ((Map<String, Object>) recObj).forEach((k, v) -> record.put(k, v != null ? v.toString() : ""));
        }
        String domain = (String) body.get("domain");
        String standard = (String) body.get("standard");

        RecordClassificationResult result = service.classifyRecord(record, domain, standard);
        return ResponseEntity.ok(buildRecordResponse(result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/eval_table")
    public ResponseEntity<Map<String, Object>> evalTable(@RequestBody Map<String, Object> body) {
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
        List<String> schema = body.containsKey("schema") ? (List<String>) body.get("schema") : null;
        String domain = (String) body.get("domain");
        String standard = (String) body.get("standard");

        TableClassificationResult result = service.classifyTable(records, schema, domain, standard);
        return ResponseEntity.ok(buildTableResponse(result));
    }

    @GetMapping("/standards")
    public ResponseEntity<Map<String, Object>> standards() {
        return ResponseEntity.ok(Map.of("standards", List.of()));
    }

    @GetMapping("/domains")
    public ResponseEntity<Map<String, Object>> domains() {
        return ResponseEntity.ok(Map.of("domains", List.of()));
    }

    @GetMapping("/operators")
    public ResponseEntity<Map<String, Object>> operators() {
        return ResponseEntity.ok(Map.of("operators", List.of()));
    }

    // ─── 响应构建辅助 ───

    private Map<String, Object> buildFieldResponse(FieldClassificationResult result) {
        Map<String, Object> resp = new LinkedHashMap<>();
        if (result == null) {
            resp.put("tags", List.of());
            resp.put("max_level", "");
            return resp;
        }
        resp.put("tags", tagsToList(result.getTags()));
        resp.put("max_level", result.getFinalLevel() != null ? result.getFinalLevel() : "");
        resp.put("confidence", result.getConfidence());
        resp.put("engine_layer", result.getEngineLayer() != null ? result.getEngineLayer() : "L1_RULE");
        resp.put("field_name", result.getFieldName());
        resp.put("audit_timestamp", Instant.now().toString());
        return resp;
    }

    private Map<String, Object> buildRecordResponse(RecordClassificationResult result) {
        Map<String, Object> resp = new LinkedHashMap<>();
        if (result == null) {
            resp.put("field_results", Map.of());
            return resp;
        }
        Map<String, Object> fieldResults = new LinkedHashMap<>();
        if (result.getFieldResults() != null) {
            result.getFieldResults().forEach((k, v) -> fieldResults.put(k, buildFieldResponse(v)));
        }
        resp.put("field_results", fieldResults);
        resp.put("max_level", result.getFinalLevel() != null ? result.getFinalLevel() : "");
        resp.put("audit_timestamp", Instant.now().toString());
        return resp;
    }

    private Map<String, Object> buildTableResponse(TableClassificationResult result) {
        Map<String, Object> resp = new LinkedHashMap<>();
        if (result == null) {
            resp.put("record_results", List.of());
            return resp;
        }
        List<Map<String, Object>> recordResults = new ArrayList<>();
        if (result.getRecordResults() != null) {
            for (RecordClassificationResult rr : result.getRecordResults()) {
                recordResults.add(buildRecordResponse(rr));
            }
        }
        resp.put("record_results", recordResults);
        resp.put("audit_timestamp", Instant.now().toString());
        return resp;
    }

    private List<Map<String, Object>> tagsToList(List<SecurityTag> tags) {
        if (tags == null) return List.of();
        return tags.stream().map(tag -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("level", tag.getLevel());
            m.put("category", tag.getCategory());
            m.put("rule_id", tag.getRuleId());
            m.put("source_engine", tag.getSource());
            m.put("domain", "");
            m.put("standard_id", "");
            m.put("is_override", false);
            m.put("is_downgrade", false);
            m.put("match_target", "");
            return m;
        }).collect(Collectors.toList());
    }
}
