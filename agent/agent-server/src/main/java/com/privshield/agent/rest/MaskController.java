package com.privshield.agent.rest;

import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 脱敏 REST 端点 — 对应 Python routers/mask.py。
 */
@RestController
@RequestMapping("/v1/privacy")
public class MaskController {

    private final PrivacyService service;

    public MaskController(PrivacyService service) {
        this.service = service;
    }

    @PostMapping("/mask")
    public ResponseEntity<Map<String, Object>> mask(@RequestBody Map<String, String> body) {
        String result = service.mask(
                body.getOrDefault("field_name", ""),
                body.getOrDefault("value", ""),
                body.getOrDefault("context", ""));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/mask_record")
    public ResponseEntity<Map<String, Object>> maskRecord(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<String, String> record = (Map<String, String>) body.getOrDefault("record", Map.of());
        String context = (String) body.getOrDefault("context", "");
        Map<String, String> result = service.maskRecord(record, context);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/mask/batch")
    public ResponseEntity<Map<String, Object>> maskBatch(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> fieldNames = (List<String>) body.getOrDefault("field_names", List.of());
        @SuppressWarnings("unchecked")
        List<String> values = (List<String>) body.getOrDefault("values", List.of());
        String context = (String) body.getOrDefault("context", "");
        List<String> result = service.maskBatch(fieldNames, values, context);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/mask/dataframe")
    public ResponseEntity<Map<String, Object>> maskDataFrame(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> data = (List<Map<String, String>>) body.getOrDefault("data", List.of());
        @SuppressWarnings("unchecked")
        List<String> columns = body.containsKey("columns") ? (List<String>) body.get("columns") : null;
        String context = (String) body.getOrDefault("context", "");
        List<Map<String, String>> result = service.maskDataFrame(data, columns, context);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/hash")
    public ResponseEntity<Map<String, Object>> hash(@RequestBody Map<String, String> body) {
        String result = service.hash(body.getOrDefault("value", ""), body.getOrDefault("salt", ""));
        return ResponseEntity.ok(Map.of("result", result));
    }
}
