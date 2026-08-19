package com.privshield.agent.rest;

import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * K-匿名 REST 端点 — 对应 Python routers/kano.py。
 */
@RestController
@RequestMapping("/v1/privacy/k_anonymize")
public class KAnonymityController {

    private final PrivacyService service;

    public KAnonymityController(PrivacyService service) {
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/record")
    public ResponseEntity<Map<String, Object>> kAnonymizeRecord(@RequestBody Map<String, Object> body) {
        Map<String, String> record = new LinkedHashMap<>();
        ((Map<String, Object>) body.getOrDefault("record", Map.of()))
                .forEach((k, v) -> record.put(k, v != null ? v.toString() : ""));
        List<String> qiCols = (List<String>) body.getOrDefault("qi_cols", List.of());
        int k = ((Number) body.getOrDefault("k", 5)).intValue();
        Map<String, String> result = service.kAnonymizeRecord(record, qiCols, k);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/table")
    public ResponseEntity<Map<String, Object>> kAnonymizeTable(@RequestBody Map<String, Object> body) {
        List<Map<String, String>> rows = new ArrayList<>();
        for (Object r : (List<?>) body.getOrDefault("rows", List.of())) {
            Map<String, String> row = new LinkedHashMap<>();
            ((Map<String, Object>) r).forEach((k, v) -> row.put(k, v != null ? v.toString() : ""));
            rows.add(row);
        }
        List<String> qiCols = (List<String>) body.getOrDefault("qi_cols", List.of());
        int k = ((Number) body.getOrDefault("k", 5)).intValue();
        int maxDepth = ((Number) body.getOrDefault("max_depth", 10)).intValue();
        List<Map<String, String>> result = service.kAnonymizeTable(rows, qiCols, k, maxDepth);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/dataframe")
    public ResponseEntity<Map<String, Object>> kAnonymizeDataFrame(@RequestBody Map<String, Object> body) {
        List<Map<String, String>> data = new ArrayList<>();
        for (Object r : (List<?>) body.getOrDefault("data", List.of())) {
            Map<String, String> row = new LinkedHashMap<>();
            ((Map<String, Object>) r).forEach((k, v) -> row.put(k, v != null ? v.toString() : ""));
            data.add(row);
        }
        List<String> qiCols = (List<String>) body.getOrDefault("qi_cols", List.of());
        int k = ((Number) body.getOrDefault("k", 5)).intValue();
        int maxDepth = ((Number) body.getOrDefault("max_depth", 10)).intValue();
        List<Map<String, String>> result = service.kAnonymizeTable(data, qiCols, k, maxDepth);
        return ResponseEntity.ok(Map.of("result", result));
    }
}
