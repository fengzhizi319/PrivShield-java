package com.privshield.agent.rest;

import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 查询混淆 REST 端点 — 对应 Python routers/qol.py。
 */
@RestController
@RequestMapping("/v1/privacy/qol")
public class QolController {

    private final PrivacyService service;

    public QolController(PrivacyService service) {
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/obfuscate")
    public ResponseEntity<Map<String, Object>> obfuscateQuery(@RequestBody Map<String, Object> body) {
        String query = (String) body.getOrDefault("query", "");
        int numDummies = body.containsKey("num_dummies") ? ((Number) body.get("num_dummies")).intValue() : 3;
        String domain = (String) body.getOrDefault("domain", "medical");
        List<String> medPool = body.containsKey("medical_pool") ? (List<String>) body.get("medical_pool") : null;
        List<String> genPool = body.containsKey("generic_pool") ? (List<String>) body.get("generic_pool") : null;
        Integer seed = body.containsKey("seed") ? ((Number) body.get("seed")).intValue() : null;
        List<String> result = service.obfuscateQuery(query, numDummies, domain, medPool, genPool, seed);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/obfuscate_batch")
    public ResponseEntity<Map<String, Object>> obfuscateQueryBatch(@RequestBody Map<String, Object> body) {
        List<String> queries = (List<String>) body.getOrDefault("queries", List.of());
        int numDummies = body.containsKey("num_dummies") ? ((Number) body.get("num_dummies")).intValue() : 3;
        String domain = (String) body.getOrDefault("domain", "medical");
        List<String> medPool = body.containsKey("medical_pool") ? (List<String>) body.get("medical_pool") : null;
        List<String> genPool = body.containsKey("generic_pool") ? (List<String>) body.get("generic_pool") : null;
        Integer seed = body.containsKey("seed") ? ((Number) body.get("seed")).intValue() : null;
        List<List<String>> result = service.obfuscateQueryBatch(queries, numDummies, domain, medPool, genPool, seed);
        return ResponseEntity.ok(Map.of("results", result));
    }
}
