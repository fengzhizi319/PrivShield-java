package com.privshield.agent.rest;

import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 本地差分隐私 REST 端点 — 对应 Python routers/ldp.py。
 */
@RestController
@RequestMapping("/v1/privacy/ldp")
public class LDPController {

    private final PrivacyService service;

    public LDPController(PrivacyService service) {
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/perturb_binary_batch")
    public ResponseEntity<Map<String, Object>> perturbBinaryBatch(@RequestBody Map<String, Object> body) {
        List<Integer> values = ((List<Number>) body.getOrDefault("values", List.of()))
                .stream().map(Number::intValue).collect(Collectors.toList());
        double epsilon = ((Number) body.getOrDefault("epsilon", 1.0)).doubleValue();
        List<Integer> result = service.perturbBinaryBatch(values, epsilon);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/perturb_categorical_batch")
    public ResponseEntity<Map<String, Object>> perturbCategoricalBatch(@RequestBody Map<String, Object> body) {
        List<String> values = (List<String>) body.getOrDefault("values", List.of());
        List<String> categories = (List<String>) body.getOrDefault("categories", List.of());
        double epsilon = ((Number) body.getOrDefault("epsilon", 1.0)).doubleValue();
        List<String> result = service.perturbCategoricalBatch(values, categories, epsilon);
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/estimate_binary_frequency")
    public ResponseEntity<Map<String, Object>> estimateBinaryFrequency(@RequestBody Map<String, Object> body) {
        List<Integer> values = ((List<Number>) body.getOrDefault("reported_values", List.of()))
                .stream().map(Number::intValue).collect(Collectors.toList());
        double epsilon = ((Number) body.getOrDefault("epsilon", 1.0)).doubleValue();
        double result = service.estimateBinaryFrequency(values, epsilon);
        return ResponseEntity.ok(Map.of("estimated_frequency", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/estimate_categorical_histogram")
    public ResponseEntity<Map<String, Object>> estimateCategoricalHistogram(@RequestBody Map<String, Object> body) {
        List<String> values = (List<String>) body.getOrDefault("reported_values", List.of());
        List<String> categories = (List<String>) body.getOrDefault("categories", List.of());
        double epsilon = ((Number) body.getOrDefault("epsilon", 1.0)).doubleValue();
        Map<String, Double> result = service.estimateCategoricalHistogram(values, categories, epsilon);
        return ResponseEntity.ok(Map.of("estimated_histogram", result));
    }
}
