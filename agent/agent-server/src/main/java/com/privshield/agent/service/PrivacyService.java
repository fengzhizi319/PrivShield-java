package com.privshield.agent.service;

import com.github.fengzhizi319.privacy.sdk.PrivacyClient;
import com.github.fengzhizi319.privacy.sdk.PrivacyProfile;
import com.github.fengzhizi319.privacy.sdk.api.KAnonymityApi;
import com.github.fengzhizi319.privacy.sdk.dynclassification.DynClassificationService;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.FieldClassificationResult;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.RecordClassificationResult;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.SecurityTag;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.TableClassificationResult;
import com.github.fengzhizi319.privacy.sdk.exception.PrivacyBudgetExhaustedException;
import com.github.fengzhizi319.privacy.sdk.util.BudgetAccountant;
import com.privshield.agent.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 隐私计算服务编排层 — 对应 Python PrivacyService。
 *
 * <p>持有 PrivacyClient 与 DynClassificationService，为 REST/gRPC handler 提供统一业务入口。</p>
 */
@Service
public class PrivacyService {
    private static final Logger log = LoggerFactory.getLogger(PrivacyService.class);

    private final AgentProperties props;
    private PrivacyClient client;
    private DynClassificationService dynService;
    private final Map<String, PrivacyClient> clientCache = new ConcurrentHashMap<>();

    public PrivacyService(AgentProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        PrivacyProfile profile = PrivacyProfile.empty();
        String ns = props.getNamespace();
        this.client = PrivacyClient.builder()
                .profile(profile)
                .namespace(ns)
                .epsilon(10.0)
                .delta(1e-4)
                .build();
        this.clientCache.put(ns, this.client);

        // 初始化动态分类服务
        DynClassificationService.ServiceConfig dynConfig = new DynClassificationService.ServiceConfig();
        dynConfig.setRulesDir(props.getRulesDir());
        this.dynService = new DynClassificationService(dynConfig);

        log.info("PrivacyService initialized: namespace={}, rulesDir={}", ns, props.getRulesDir());
    }

    // ─── 脱敏 / Masking ───

    public String mask(String fieldName, String value, String context) {
        return client.maskValue(fieldName, value, context);
    }

    public Map<String, String> maskRecord(Map<String, String> record, String context) {
        @SuppressWarnings("unchecked")
        Map<String, Object> objRecord = new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) record);
        Map<String, Object> result = client.maskRecord(objRecord, context);
        Map<String, String> strResult = new LinkedHashMap<>();
        result.forEach((k, v) -> strResult.put(k, v != null ? v.toString() : ""));
        return strResult;
    }

    public List<String> maskBatch(List<String> fieldNames, List<String> values, String context) {
        return client.maskBatch(fieldNames, values, context);
    }

    public List<Map<String, String>> maskDataFrame(List<Map<String, String>> data, List<String> columns, String context) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map<String, String> row : data) {
            result.add(maskRecord(row, context));
        }
        return result;
    }

    public String hash(String value, String salt) {
        return client.hashValue(value, salt);
    }

    // ─── 差分隐私 / Differential Privacy ───

    public double dpCount(List<Double> values, double epsilon, double delta, String mechanism) {
        return client.dp().count(values, epsilon, delta, mechanism);
    }

    public double dpSum(List<Double> values, double epsilon, double delta, String mechanism,
                         Double clipLower, Double clipUpper) {
        return client.dp().sum(values, epsilon, delta, mechanism, clipLower, clipUpper);
    }

    public double dpMean(List<Double> values, double epsilon, double delta, String mechanism,
                          Double clipLower, Double clipUpper) {
        return client.dp().mean(values, epsilon, delta, mechanism, clipLower, clipUpper);
    }

    public Map<String, Double> dpHistogram(List<String> values, List<String> categories,
                                            double epsilon, double delta, String mechanism) {
        return client.dp().histogram(values, categories, epsilon, delta, mechanism);
    }

    // ─── Noisy DP (对已聚合值加噪) ───

    public double dpNoisyCount(double trueCount, double epsilon, double delta, String mechanism) {
        return client.dpNoisyCount(trueCount, epsilon, delta, mechanism);
    }

    public double dpNoisySum(double trueSum, double sensitivity, double epsilon, double delta, String mechanism) {
        return client.dpNoisySum(trueSum, sensitivity, epsilon, delta, mechanism);
    }

    public double dpNoisyMean(double trueSum, double trueCount, double sensitivity,
                               double epsilon, double delta, String mechanism, double minCount) {
        return client.dpNoisyMean(trueSum, trueCount, sensitivity, epsilon, delta, mechanism, minCount);
    }

    public Map<String, Double> dpNoisyHistogram(Map<String, Double> trueCounts,
                                                  double epsilon, double delta, String mechanism) {
        return client.dpNoisyHistogram(trueCounts, epsilon, delta, mechanism);
    }

    // ─── 分块流式 DP (Chunked) ───

    public double dpChunkedCount(List<List<Double>> chunks, double epsilon, double delta, String mechanism) {
        // 对每个 chunk 局部计数后汇总，再加噪
        double totalCount = 0;
        for (List<Double> chunk : chunks) {
            totalCount += chunk.size();
        }
        return client.dpNoisyCount(totalCount, epsilon, delta, mechanism);
    }

    public double dpChunkedSum(List<List<Double>> chunks, double epsilon, double delta, String mechanism,
                                Double clipLower, Double clipUpper) {
        double totalSum = 0;
        for (List<Double> chunk : chunks) {
            for (double v : chunk) {
                double clipped = v;
                if (clipLower != null && clipped < clipLower) clipped = clipLower;
                if (clipUpper != null && clipped > clipUpper) clipped = clipUpper;
                totalSum += clipped;
            }
        }
        double sensitivity = (clipLower != null && clipUpper != null) ? clipUpper - clipLower : 1.0;
        return client.dpNoisySum(totalSum, sensitivity, epsilon, delta, mechanism);
    }

    public double dpChunkedMean(List<List<Double>> chunks, double epsilon, double delta, String mechanism,
                                 Double clipLower, Double clipUpper, double minCount) {
        double totalSum = 0;
        int totalCount = 0;
        for (List<Double> chunk : chunks) {
            for (double v : chunk) {
                double clipped = v;
                if (clipLower != null && clipped < clipLower) clipped = clipLower;
                if (clipUpper != null && clipped > clipUpper) clipped = clipUpper;
                totalSum += clipped;
                totalCount++;
            }
        }
        if (totalCount < minCount) return 0.0;
        double sensitivity = (clipLower != null && clipUpper != null) ? clipUpper - clipLower : 1.0;
        return client.dpNoisyMean(totalSum, totalCount, sensitivity, epsilon, delta, mechanism, minCount);
    }

    public Map<String, Double> dpChunkedHistogram(List<List<String>> chunks, List<String> categories,
                                                    double epsilon, double delta, String mechanism) {
        Map<String, Double> trueCounts = new LinkedHashMap<>();
        for (String cat : categories) trueCounts.put(cat, 0.0);
        for (List<String> chunk : chunks) {
            for (String v : chunk) {
                trueCounts.merge(v, 1.0, Double::sum);
            }
        }
        return client.dpNoisyHistogram(trueCounts, epsilon, delta, mechanism);
    }

    // ─── K-匿名 / K-Anonymity ───

    public Map<String, String> kAnonymizeRecord(Map<String, String> record, List<String> qiCols, int k) {
        @SuppressWarnings("unchecked")
        Map<String, Object> objRecord = new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) record);
        Map<String, Object> result = client.kAnonymizeRecord(objRecord, qiCols, null, k);
        Map<String, String> strResult = new LinkedHashMap<>();
        result.forEach((k2, v) -> strResult.put(k2, v != null ? v.toString() : ""));
        return strResult;
    }

    public List<Map<String, String>> kAnonymizeTable(List<Map<String, String>> rows, List<String> qiCols,
                                                       int k, int maxDepth) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> objRows = rows.stream()
                .map(r -> new LinkedHashMap<>((Map<String, Object>) (Map<?, ?>) r))
                .collect(Collectors.toList());
        List<Map<String, Object>> result = client.kAnonymizeTable(objRows, qiCols, k, maxDepth);
        return result.stream().map(r -> {
            Map<String, String> m = new LinkedHashMap<>();
            r.forEach((k2, v) -> m.put(k2, v != null ? v.toString() : ""));
            return m;
        }).collect(Collectors.toList());
    }

    // ─── 查询混淆 / Query Obfuscation ───

    public List<String> obfuscateQuery(String query, int numDummies, String domain,
                                        List<String> medicalPool, List<String> genericPool, Integer seed) {
        return client.obfuscateQuery(query, numDummies, domain, medicalPool, genericPool);
    }

    public List<List<String>> obfuscateQueryBatch(List<String> queries, int numDummies, String domain,
                                                    List<String> medicalPool, List<String> genericPool, Integer seed) {
        return client.obfuscateQueryBatch(queries, numDummies, domain, medicalPool, genericPool);
    }

    // ─── 本地 DP / Local DP ───

    public List<Integer> perturbBinaryBatch(List<Integer> values, double epsilon) {
        return client.perturbBinaryBatch(values, epsilon);
    }

    public List<String> perturbCategoricalBatch(List<String> values, List<String> categories, double epsilon) {
        return client.perturbCategoricalBatch(values, categories, epsilon);
    }

    public double estimateBinaryFrequency(List<Integer> reported, double epsilon) {
        return client.estimateBinaryFrequency(reported, epsilon);
    }

    public Map<String, Double> estimateCategoricalHistogram(List<String> reported, List<String> categories, double epsilon) {
        return client.estimateCategoricalHistogram(reported, categories, epsilon);
    }

    // ─── 高级 DP ───

    public double[] dpVectorSum(List<double[]> vectors, double maxNorm, double epsilon, double delta, String mechanism) {
        return client.dpVectorSum(vectors, maxNorm, epsilon, delta, mechanism);
    }

    public double[] dpVectorMean(List<double[]> vectors, double maxNorm, double epsilon, double delta,
                                  String mechanism, double minCount) {
        return client.dpVectorMean(vectors, maxNorm, epsilon, delta, mechanism, minCount);
    }

    public double[] dpAdaptiveClip(List<Double> values, double epsilon, double targetQuantile,
                                    int numIterations, double initialClip) {
        return client.dpAdaptiveClip(values, epsilon, targetQuantile, numIterations, initialClip);
    }

    public Map<String, Double> dpGroupBy(List<Map<String, Object>> rows, String groupCol, String targetCol,
                                          String agg, double epsilon, double delta,
                                          Double clipLower, Double clipUpper, String mechanism) {
        return client.dpGroupBy(rows, groupCol, targetCol, agg, epsilon, delta, clipLower, clipUpper, mechanism);
    }

    // ─── 动态分类 / Dynamic Classification ───

    public FieldClassificationResult classifyField(String fieldName, String value, String domain, String standard) {
        return dynService.classifyField(fieldName, value, domain, standard);
    }

    public RecordClassificationResult classifyRecord(Map<String, String> record, String domain, String standard) {
        return dynService.classifyRecord(record, domain, standard);
    }

    public TableClassificationResult classifyTable(List<Map<String, String>> records, List<String> schema,
                                                    String domain, String standard) {
        return dynService.classifyTable(records, schema, domain, standard);
    }

    // ─── 预算 / Budget ───

    public Map<String, Double> budgetRemaining() {
        return client.budgetRemaining();
    }

    // ─── 参数推荐 / Recommend Params ───

    public Map<String, Object> recommendParams(List<Double> values, List<Map<String, Object>> rows,
                                                List<String> qiCols, String namespace) {
        PrivacyClient nsClient = clientCache.computeIfAbsent(namespace, ns ->
                PrivacyClient.builder().namespace(ns).build());
        return nsClient.recommendAndSaveParams(values, rows, qiCols);
    }

    /**
     * 获取 client（供 REST controller 使用）。
     */
    public PrivacyClient getClient() {
        return client;
    }

    /**
     * 获取 DynClassificationService（供 REST controller 使用）。
     */
    public DynClassificationService getDynService() {
        return dynService;
    }
}
