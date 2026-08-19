package com.privshield.agent.rest;

import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 差分隐私 REST 端点 — 对应 Python routers/dp.py。
 */
@RestController
@RequestMapping("/v1/privacy/dp")
public class DPController {

    private final PrivacyService service;

    public DPController(PrivacyService service) {
        this.service = service;
    }

    // ─── 辅助方法 ───

    private double getParam(Map<String, Object> params, String key, double defaultVal) {
        if (params == null || !params.containsKey(key)) return defaultVal;
        Object v = params.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return defaultVal;
    }

    private String getParam(Map<String, Object> params, String key, String defaultVal) {
        if (params == null || !params.containsKey(key)) return defaultVal;
        return String.valueOf(params.get(key));
    }

    private Double getParamOrNull(Map<String, Object> params, String key) {
        if (params == null || !params.containsKey(key)) return null;
        Object v = params.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Double> getValues(Map<String, Object> body) {
        Object v = body.get("values");
        if (v instanceof List) return ((List<Number>) v).stream().map(Number::doubleValue).collect(Collectors.toList());
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getParams(Map<String, Object> body) {
        Object p = body.get("params");
        if (p instanceof Map) return (Map<String, Object>) p;
        return Map.of();
    }

    // ─── 基础 DP 聚合 ───

    @PostMapping("/count")
    public ResponseEntity<Map<String, Object>> dpCount(@RequestBody Map<String, Object> body) {
        List<Double> values = getValues(body);
        Map<String, Object> params = getParams(body);
        double result = service.dpCount(values,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/sum")
    public ResponseEntity<Map<String, Object>> dpSum(@RequestBody Map<String, Object> body) {
        List<Double> values = getValues(body);
        Map<String, Object> params = getParams(body);
        double result = service.dpSum(values,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"),
                getParamOrNull(params, "clip_lower"),
                getParamOrNull(params, "clip_upper"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/mean")
    public ResponseEntity<Map<String, Object>> dpMean(@RequestBody Map<String, Object> body) {
        List<Double> values = getValues(body);
        Map<String, Object> params = getParams(body);
        double result = service.dpMean(values,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"),
                getParamOrNull(params, "clip_lower"),
                getParamOrNull(params, "clip_upper"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/histogram")
    public ResponseEntity<Map<String, Object>> dpHistogram(@RequestBody Map<String, Object> body) {
        List<String> values = (List<String>) body.getOrDefault("values", List.of());
        List<String> categories = (List<String>) body.getOrDefault("categories", List.of());
        Map<String, Object> params = getParams(body);
        Map<String, Double> result = service.dpHistogram(values, categories,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    // ─── Noisy DP ───

    @PostMapping("/noisy_count")
    public ResponseEntity<Map<String, Object>> dpNoisyCount(@RequestBody Map<String, Object> body) {
        Map<String, Object> params = getParams(body);
        double result = service.dpNoisyCount(
                getParam(body, "true_count", 0),
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/noisy_sum")
    public ResponseEntity<Map<String, Object>> dpNoisySum(@RequestBody Map<String, Object> body) {
        Map<String, Object> params = getParams(body);
        double sensitivity = getParam(params, "sensitivity", 0);
        if (sensitivity == 0) {
            sensitivity = getParam(params, "clip_upper", 1.0) - getParam(params, "clip_lower", 0.0);
        }
        double result = service.dpNoisySum(
                getParam(body, "true_sum", 0), sensitivity,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @PostMapping("/noisy_mean")
    public ResponseEntity<Map<String, Object>> dpNoisyMean(@RequestBody Map<String, Object> body) {
        Map<String, Object> params = getParams(body);
        double sensitivity = getParam(params, "sensitivity", 0);
        if (sensitivity == 0) {
            sensitivity = getParam(params, "clip_upper", 1.0) - getParam(params, "clip_lower", 0.0);
        }
        double result = service.dpNoisyMean(
                getParam(body, "true_sum", 0),
                getParam(body, "true_count", 0), sensitivity,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"),
                getParam(params, "min_count", 5.0));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/noisy_histogram")
    public ResponseEntity<Map<String, Object>> dpNoisyHistogram(@RequestBody Map<String, Object> body) {
        Map<String, Double> trueCounts = new HashMap<>();
        Object tc = body.get("true_counts");
        if (tc instanceof Map) {
            ((Map<String, Object>) tc).forEach((k, v) -> {
                if (v instanceof Number) trueCounts.put(k, ((Number) v).doubleValue());
            });
        }
        Map<String, Object> params = getParams(body);
        Map<String, Double> result = service.dpNoisyHistogram(trueCounts,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    // ─── Chunked DP ───

    @SuppressWarnings("unchecked")
    @PostMapping("/chunked/count")
    public ResponseEntity<Map<String, Object>> dpChunkedCount(@RequestBody Map<String, Object> body) {
        List<List<Double>> chunks = extractDoubleChunks(body.get("chunks"));
        Map<String, Object> params = getParams(body);
        double result = service.dpChunkedCount(chunks,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/chunked/sum")
    public ResponseEntity<Map<String, Object>> dpChunkedSum(@RequestBody Map<String, Object> body) {
        List<List<Double>> chunks = extractDoubleChunks(body.get("chunks"));
        Map<String, Object> params = getParams(body);
        double result = service.dpChunkedSum(chunks,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"),
                getParamOrNull(params, "clip_lower"),
                getParamOrNull(params, "clip_upper"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/chunked/mean")
    public ResponseEntity<Map<String, Object>> dpChunkedMean(@RequestBody Map<String, Object> body) {
        List<List<Double>> chunks = extractDoubleChunks(body.get("chunks"));
        Map<String, Object> params = getParams(body);
        double result = service.dpChunkedMean(chunks,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"),
                getParamOrNull(params, "clip_lower"),
                getParamOrNull(params, "clip_upper"),
                getParam(params, "min_count", 5.0));
        return ResponseEntity.ok(Map.of("result", result));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/chunked/histogram")
    public ResponseEntity<Map<String, Object>> dpChunkedHistogram(@RequestBody Map<String, Object> body) {
        List<List<String>> chunks = extractStringChunks(body.get("chunks"));
        List<String> categories = (List<String>) body.getOrDefault("categories", List.of());
        Map<String, Object> params = getParams(body);
        Map<String, Double> result = service.dpChunkedHistogram(chunks, categories,
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    // ─── 高级 DP ───

    @SuppressWarnings("unchecked")
    @PostMapping("/vector_sum")
    public ResponseEntity<Map<String, Object>> dpVectorSum(@RequestBody Map<String, Object> body) {
        List<double[]> vectors = extractVectors(body.get("vectors"));
        Map<String, Object> params = getParams(body);
        double[] result = service.dpVectorSum(vectors,
                getParam(params, "max_norm", 1.0),
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "gaussian"));
        List<Double> resultList = Arrays.stream(result).boxed().collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("result", resultList));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/vector_mean")
    public ResponseEntity<Map<String, Object>> dpVectorMean(@RequestBody Map<String, Object> body) {
        List<double[]> vectors = extractVectors(body.get("vectors"));
        Map<String, Object> params = getParams(body);
        double[] result = service.dpVectorMean(vectors,
                getParam(params, "max_norm", 1.0),
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 0.0),
                getParam(params, "mechanism", "gaussian"),
                getParam(params, "min_count", 5.0));
        List<Double> resultList = Arrays.stream(result).boxed().collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("result", resultList));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/adaptive_clip")
    public ResponseEntity<Map<String, Object>> dpAdaptiveClip(@RequestBody Map<String, Object> body) {
        List<Double> values = getValues(body);
        Map<String, Object> params = getParams(body);
        double[] result = service.dpAdaptiveClip(values,
                getParam(params, "epsilon", 1.0),
                getParam(params, "target_quantile", 0.95),
                (int) getParam(params, "num_iterations", 15),
                getParam(params, "initial_clip", 10.0));
        return ResponseEntity.ok(Map.of("clip_lower", result[0], "clip_upper", result[1]));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/groupby")
    public ResponseEntity<Map<String, Object>> dpGroupBy(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> rows = (List<Map<String, Object>>) body.getOrDefault("rows", List.of());
        Map<String, Object> params = getParams(body);
        Map<String, Double> result = service.dpGroupBy(rows,
                (String) body.getOrDefault("group_col", ""),
                (String) body.getOrDefault("target_col", ""),
                (String) body.getOrDefault("agg", "count"),
                getParam(params, "epsilon", 1.0),
                getParam(params, "delta", 1e-5),
                getParamOrNull(params, "clip_lower"),
                getParamOrNull(params, "clip_upper"),
                getParam(params, "mechanism", "laplace"));
        return ResponseEntity.ok(Map.of("result", result));
    }

    // ─── 辅助提取方法 ───

    @SuppressWarnings("unchecked")
    private List<List<Double>> extractDoubleChunks(Object chunksObj) {
        List<List<Double>> chunks = new ArrayList<>();
        if (chunksObj instanceof List) {
            for (Object chunk : (List<?>) chunksObj) {
                if (chunk instanceof Map) {
                    Object vals = ((Map<String, Object>) chunk).get("values");
                    if (vals instanceof List) {
                        chunks.add(((List<Number>) vals).stream().map(Number::doubleValue).collect(Collectors.toList()));
                    }
                } else if (chunk instanceof List) {
                    chunks.add(((List<Number>) chunk).stream().map(Number::doubleValue).collect(Collectors.toList()));
                }
            }
        }
        return chunks;
    }

    @SuppressWarnings("unchecked")
    private List<List<String>> extractStringChunks(Object chunksObj) {
        List<List<String>> chunks = new ArrayList<>();
        if (chunksObj instanceof List) {
            for (Object chunk : (List<?>) chunksObj) {
                if (chunk instanceof Map) {
                    Object vals = ((Map<String, Object>) chunk).get("values");
                    if (vals instanceof List) {
                        chunks.add(((List<?>) vals).stream().map(Object::toString).collect(Collectors.toList()));
                    }
                } else if (chunk instanceof List) {
                    chunks.add(((List<?>) chunk).stream().map(Object::toString).collect(Collectors.toList()));
                }
            }
        }
        return chunks;
    }

    @SuppressWarnings("unchecked")
    private List<double[]> extractVectors(Object vectorsObj) {
        List<double[]> vectors = new ArrayList<>();
        if (vectorsObj instanceof List) {
            for (Object v : (List<?>) vectorsObj) {
                if (v instanceof Map) {
                    Object vals = ((Map<String, Object>) v).get("values");
                    if (vals instanceof List) {
                        vectors.add(((List<Number>) vals).stream().mapToDouble(Number::doubleValue).toArray());
                    }
                } else if (v instanceof List) {
                    vectors.add(((List<Number>) v).stream().mapToDouble(Number::doubleValue).toArray());
                }
            }
        }
        return vectors;
    }
}
