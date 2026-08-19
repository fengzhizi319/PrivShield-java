package com.github.fengzhizi319.privacy.sdk.dynclassification.llm;

import com.github.fengzhizi319.privacy.sdk.dynclassification.model.DomainTaxonomy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter for LLM classifiers with lazy initialization.
 * LLM 分类器的延迟初始化适配器。
 */
public class LlmAdapter {
    private static final Logger log = LoggerFactory.getLogger(LlmAdapter.class);

    private final String endpoint;
    private final String model;
    private final String systemPrompt;
    private volatile LlmClassifier classifier;
    private volatile boolean initialized = false;

    public LlmAdapter(String endpoint, String model, String systemPrompt) {
        this.endpoint = endpoint;
        this.model = model;
        this.systemPrompt = systemPrompt;
    }

    /**
     * Classifies a field using the LLM.
     * 使用 LLM 对字段进行分类。
     */
    public String classify(String fieldName, String fieldValue, DomainTaxonomy taxonomy) {
        LlmClassifier c = getClassifier();
        if (c == null || !c.isAvailable()) {
            return null;
        }
        try {
            return c.classify(fieldName, fieldValue, taxonomy);
        } catch (Exception e) {
            log.warn("LLM classification failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Arbitrates between rule result and LLM result.
     * 在规则结果和 LLM 结果之间进行仲裁。
     */
    public String arbitrate(String ruleLevel, String llmLevel, DomainTaxonomy taxonomy) {
        if (llmLevel == null || llmLevel.isEmpty()) {
            return ruleLevel;
        }
        // Trust LLM if it suggests higher sensitivity
        int ruleRank = ruleLevel != null ? taxonomy.getLevelRank(ruleLevel) : 0;
        int llmRank = taxonomy.getLevelRank(llmLevel);
        return llmRank > ruleRank ? llmLevel : ruleLevel;
    }

    private LlmClassifier getClassifier() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    classifier = createClassifier();
                    initialized = true;
                }
            }
        }
        return classifier;
    }

    private LlmClassifier createClassifier() {
        if (endpoint == null || endpoint.isEmpty()) {
            return new NoOpLlmClassifier();
        }
        HttpLlmClassifier httpClassifier = new HttpLlmClassifier(endpoint, model, systemPrompt);
        if (httpClassifier.isAvailable()) {
            log.info("HTTP LLM classifier initialized for endpoint: {}", endpoint);
            return httpClassifier;
        }
        return new NoOpLlmClassifier();
    }

    /**
     * Resets the adapter for reload.
     * 重置适配器以便重新加载。
     */
    public void reset() {
        synchronized (this) {
            initialized = false;
            classifier = null;
        }
    }
}
