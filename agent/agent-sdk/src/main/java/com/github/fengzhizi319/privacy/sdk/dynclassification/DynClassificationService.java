package com.github.fengzhizi319.privacy.sdk.dynclassification;

import com.github.fengzhizi319.privacy.sdk.dynclassification.engine.ClassificationFunnel;
import com.github.fengzhizi319.privacy.sdk.dynclassification.engine.CompositeRuleEngine;
import com.github.fengzhizi319.privacy.sdk.dynclassification.engine.ConfigurableRuleEngine;
import com.github.fengzhizi319.privacy.sdk.dynclassification.llm.LlmAdapter;
import com.github.fengzhizi319.privacy.sdk.dynclassification.loader.ProfileLoader;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.*;
import com.github.fengzhizi319.privacy.sdk.dynclassification.ner.NerAdapter;
import com.github.fengzhizi319.privacy.sdk.dynclassification.operator.OperatorRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * High-level API for dynamic data classification.
 * 动态数据分类分级的高层 API 入口。
 */
public class DynClassificationService {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ProfileLoader loader;
    private final ServiceConfig config;
    private final Map<String, ClassificationFunnel> funnels = new ConcurrentHashMap<>();
    private final Map<String, NerAdapter> nerAdapters = new ConcurrentHashMap<>();
    private final LlmAdapter llmAdapter;

    public DynClassificationService(ServiceConfig config) {
        this.config = config;
        this.loader = new ProfileLoader(config.getRulesDir());
        
        if (config.getLlmEndpoint() != null && !config.getLlmEndpoint().isEmpty()) {
            this.llmAdapter = new LlmAdapter(config.getLlmEndpoint(), config.getLlmModel(), config.getLlmPrompt());
        } else {
            this.llmAdapter = null;
        }
    }

    /**
     * Classifies a single field.
     * 对单个字段进行分类。
     */
    public FieldClassificationResult classifyField(String fieldName, String value, String domain, String standard) {
        ClassificationFunnel funnel = getFunnel(domain, standard);
        
        ClassificationFunnel.FunnelResult result = funnel.classifyField(fieldName, value);
        
        FieldClassificationResult fieldResult = new FieldClassificationResult();
        fieldResult.setFieldName(fieldName);
        fieldResult.setFieldValue(truncateForDisplay(value, 100));
        fieldResult.setTags(result.getTags() != null ? result.getTags() : new ArrayList<>());
        fieldResult.setFinalLevel(result.getFinalLevel());
        fieldResult.setConfidence(result.getConfidence());
        fieldResult.setNeedsHumanReview(result.isNeedsHumanReview());
        fieldResult.setEngineLayer(result.getEngineLayer());
        fieldResult.setReasoning(result.getReasoning());
        fieldResult.setSuppressedTags(result.getSuppressedTags() != null ? result.getSuppressedTags() : new ArrayList<>());
        
        return fieldResult;
    }

    /**
     * Classifies a record (multiple fields).
     * 对单条记录（多个字段）进行分类。
     */
    public RecordClassificationResult classifyRecord(Map<String, String> record, String domain, String standard) {
        ClassificationFunnel funnel = getFunnel(domain, standard);
        DomainTaxonomy taxonomy = funnel.taxonomy;

        Map<String, FieldClassificationResult> fieldResults = new HashMap<>();
        List<SecurityTag> allTags = new ArrayList<>();
        double maxConfidence = 0.0;
        boolean needsReview = false;

        for (Map.Entry<String, String> entry : record.entrySet()) {
            ClassificationFunnel.FunnelResult result = funnel.classifyField(entry.getKey(), entry.getValue());
            
            FieldClassificationResult fr = new FieldClassificationResult();
            fr.setFieldName(entry.getKey());
            fr.setFieldValue(truncateForDisplay(entry.getValue(), 100));
            fr.setTags(result.getTags() != null ? result.getTags() : new ArrayList<>());
            fr.setFinalLevel(result.getFinalLevel());
            fr.setConfidence(result.getConfidence());
            fr.setNeedsHumanReview(result.isNeedsHumanReview());
            fr.setEngineLayer(result.getEngineLayer());
            fr.setReasoning(result.getReasoning());
            fr.setSuppressedTags(result.getSuppressedTags() != null ? result.getSuppressedTags() : new ArrayList<>());
            
            fieldResults.put(entry.getKey(), fr);
            allTags.addAll(result.getTags());
            maxConfidence = Math.max(maxConfidence, result.getConfidence());
            needsReview = needsReview || result.isNeedsHumanReview();
        }

        // Apply composite rules
        String maxLevel = null;
        CompositeRuleEngine compositeEngine = loader.getCompositeEngine(domain, standard);
        if (compositeEngine != null) {
            List<SecurityTag> compositeTags = compositeEngine.evaluate(record);
            if (!compositeTags.isEmpty()) {
                allTags.addAll(compositeTags);
            }
            maxLevel = compositeEngine.applyToRecordLevel(maxLevel, compositeTags, taxonomy);
        }

        // Resolve final level
        if (maxLevel == null || maxLevel.isEmpty()) {
            if (!allTags.isEmpty()) {
                String[] levels = allTags.stream().map(SecurityTag::getLevel).toArray(String[]::new);
                maxLevel = taxonomy.maxLevel(levels);
            } else {
                maxLevel = taxonomy.getDefaultLevel();
            }
        }

        RecordClassificationResult recordResult = new RecordClassificationResult();
        recordResult.setFieldResults(fieldResults);
        recordResult.setAggregatedTags(allTags);
        recordResult.setFinalLevel(maxLevel);
        recordResult.setConfidence(maxConfidence);
        recordResult.setNeedsHumanReview(needsReview);
        
        return recordResult;
    }

    /**
     * Classifies an entire table.
     * 对整张表进行分类。
     */
    public TableClassificationResult classifyTable(List<Map<String, String>> records, List<String> schema,
                                                    String domain, String standard) {
        ClassificationFunnel funnel = getFunnel(domain, standard);
        DomainTaxonomy taxonomy = funnel.taxonomy;

        List<RecordClassificationResult> recordResults = new ArrayList<>();
        List<SecurityTag> allTags = new ArrayList<>();
        double maxConfidence = 0.0;
        boolean needsReview = false;
        String maxLevel = null;

        for (int i = 0; i < records.size(); i++) {
            RecordClassificationResult rr = classifyRecord(records.get(i), domain, standard);
            rr.setRecordIndex(i);
            recordResults.add(rr);
            allTags.addAll(rr.getAggregatedTags());
            maxConfidence = Math.max(maxConfidence, rr.getConfidence());
            needsReview = needsReview || rr.isNeedsHumanReview();
            
            if (maxLevel == null || taxonomy.getLevelRank(rr.getFinalLevel()) > taxonomy.getLevelRank(maxLevel)) {
                maxLevel = rr.getFinalLevel();
            }
        }

        if (maxLevel == null || maxLevel.isEmpty()) {
            maxLevel = taxonomy.getDefaultLevel();
        }

        TableClassificationResult tableResult = new TableClassificationResult();
        tableResult.setSchema(schema);
        tableResult.setRecordResults(recordResults);
        tableResult.setAggregatedTags(allTags);
        tableResult.setFinalLevel(maxLevel);
        tableResult.setConfidence(maxConfidence);
        tableResult.setNeedsHumanReview(needsReview);
        
        return tableResult;
    }

    /**
     * Performs a classification preview without side effects.
     * 执行无副作用的分类预览。
     */
    public Map<String, FieldClassificationResult> dryRun(Map<String, String> fields, String domain, String standard) {
        Map<String, FieldClassificationResult> results = new HashMap<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            results.put(entry.getKey(), classifyField(entry.getKey(), entry.getValue(), domain, standard));
        }
        return results;
    }

    /**
     * Clears all caches and resets adapters.
     * 清除所有缓存并重置适配器。
     */
    public void reload() {
        lock.writeLock().lock();
        try {
            loader.invalidateCache();
            funnels.clear();
            for (NerAdapter adapter : nerAdapters.values()) {
                adapter.reset();
            }
            nerAdapters.clear();
            if (llmAdapter != null) {
                llmAdapter.reset();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lists available standard names.
     * 列出可用的标准名称。
     */
    public List<String> listStandards() {
        return loader.listStandards();
    }

    /**
     * Lists available domain names.
     * 列出可用的领域名称。
     */
    public List<String> listDomains() {
        return loader.listDomains();
    }

    /**
     * Lists registered operator names.
     * 列出已注册的算子名称。
     */
    public List<String> listOperators() {
        return OperatorRegistry.DEFAULT.listOperators();
    }

    // --- Internal ---

    private ClassificationFunnel getFunnel(String domain, String standard) {
        String cacheKey = engineCacheKey(domain, standard);
        
        ClassificationFunnel cached = funnels.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        lock.writeLock().lock();
        try {
            cached = funnels.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            ConfigurableRuleEngine engine = loader.getEngine(domain, standard);
            DomainTaxonomy taxonomy = engine.getTaxonomy();
            ConfidencePolicy policy = taxonomy.getConfidencePolicy();

            // Build NER adapter if configured
            NerAdapter nerAdapter = null;
            if (policy != null && policy.isEnableNer()) {
                String nerKey = cacheKey + ":ner";
                nerAdapter = nerAdapters.get(nerKey);
                if (nerAdapter == null) {
                    String modelPath = config.getNerModelPath();
                    if (taxonomy.getNerModelPath() != null && !taxonomy.getNerModelPath().isEmpty()) {
                        modelPath = taxonomy.getNerModelPath();
                    }
                    String vocabPath = config.getNerVocabPath();
                    if (taxonomy.getNerVocabPath() != null && !taxonomy.getNerVocabPath().isEmpty()) {
                        vocabPath = taxonomy.getNerVocabPath();
                    }
                    nerAdapter = new NerAdapter(modelPath, vocabPath, taxonomy.getNerLabelMapping());
                    nerAdapters.put(nerKey, nerAdapter);
                }
            }

            ClassificationFunnel funnel = new ClassificationFunnel(engine, taxonomy, policy, nerAdapter, llmAdapter);
            funnels.put(cacheKey, funnel);
            return funnel;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private String truncateForDisplay(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private String engineCacheKey(String domain, String standard) {
        return (domain != null ? domain : "") + ":" + (standard != null ? standard : "");
    }

    /**
     * Service configuration.
     * 服务配置。
     */
    public static class ServiceConfig {
        private String rulesDir;
        private String nerModelPath;
        private String nerVocabPath;
        private String llmEndpoint;
        private String llmModel;
        private String llmPrompt;

        public String getRulesDir() { return rulesDir; }
        public void setRulesDir(String rulesDir) { this.rulesDir = rulesDir; }

        public String getNerModelPath() { return nerModelPath; }
        public void setNerModelPath(String nerModelPath) { this.nerModelPath = nerModelPath; }

        public String getNerVocabPath() { return nerVocabPath; }
        public void setNerVocabPath(String nerVocabPath) { this.nerVocabPath = nerVocabPath; }

        public String getLlmEndpoint() { return llmEndpoint; }
        public void setLlmEndpoint(String llmEndpoint) { this.llmEndpoint = llmEndpoint; }

        public String getLlmModel() { return llmModel; }
        public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

        public String getLlmPrompt() { return llmPrompt; }
        public void setLlmPrompt(String llmPrompt) { this.llmPrompt = llmPrompt; }
    }
}
