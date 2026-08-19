package com.github.fengzhizi319.privacy.sdk.dynclassification.engine;

import com.github.fengzhizi319.privacy.sdk.dynclassification.llm.LlmAdapter;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.*;
import com.github.fengzhizi319.privacy.sdk.dynclassification.ner.NerAdapter;
import com.github.fengzhizi319.privacy.sdk.dynclassification.ner.NerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Three-layer classification funnel: Rule Engine -> NER -> LLM.
 * 三层分类漏斗：规则引擎 -> NER -> LLM。
 */
public class ClassificationFunnel {
    private final ConfigurableRuleEngine ruleEngine;
    public final DomainTaxonomy taxonomy;
    private final ConfidencePolicy policy;
    private final NerAdapter nerAdapter;
    private final LlmAdapter llmAdapter;

    public ClassificationFunnel(ConfigurableRuleEngine ruleEngine, DomainTaxonomy taxonomy,
                                 ConfidencePolicy policy, NerAdapter nerAdapter, LlmAdapter llmAdapter) {
        this.ruleEngine = ruleEngine;
        this.taxonomy = taxonomy;
        this.policy = policy != null ? policy : new ConfidencePolicy();
        this.nerAdapter = nerAdapter;
        this.llmAdapter = llmAdapter;
    }

    /**
     * Classifies a single field through the funnel.
     * 通过漏斗对单个字段进行分类。
     */
    public FunnelResult classifyField(String fieldName, String fieldValue) {
        // Step 1: Rule engine evaluation
        ConfigurableRuleEngine.EvaluateResult ruleResult = ruleEngine.evaluate(fieldName, fieldValue);
        List<SecurityTag> tags = new ArrayList<>(ruleResult.getTags());
        List<SecurityTag> suppressedTags = ruleResult.getSuppressedTags();

        // Step 2: Conflict detection
        boolean hasConflict = detectConflict(tags);

        // Step 3: NER layer (if enabled and confidence low or conflict)
        String engineLayer = EngineLayer.RULE;
        double confidence = policy.getRuleConfidence();
        boolean needsReview = false;

        if (policy.isEnableNer() && nerAdapter != null && (tags.isEmpty() || hasConflict)) {
            List<NerEntity> entities = nerAdapter.extract(fieldName, fieldValue);
            List<SecurityTag> nerTags = convertNerEntities(entities);
            if (!nerTags.isEmpty()) {
                tags.addAll(nerTags);
                engineLayer = EngineLayer.NER;
                confidence = policy.getNerConfidence();
            }
        }

        // Step 4: LLM layer (if enabled and confidence below threshold)
        if (policy.isEnableLlm() && llmAdapter != null && confidence < policy.getLlmTriggerThreshold()) {
            String llmLevel = llmAdapter.classify(fieldName, fieldValue, taxonomy);
            if (llmLevel != null && !llmLevel.isEmpty()) {
                tags.add(new SecurityTag(llmLevel, "llm-classified", EngineLayer.LLM, policy.getLlmConfidence(), "llm"));
                engineLayer = EngineLayer.LLM;
                confidence = policy.getLlmConfidence();
            }
        }

        // Step 5: Resolve final level
        String finalLevel = resolveLevel(tags);
        if (tags.isEmpty()) {
            needsReview = confidence < 0.5;
        }

        return new FunnelResult(tags, suppressedTags, finalLevel, confidence, engineLayer, needsReview, "");
    }

    private boolean detectConflict(List<SecurityTag> tags) {
        if (tags.size() < 2) {
            return false;
        }
        int minRank = Integer.MAX_VALUE;
        int maxRank = Integer.MIN_VALUE;
        for (SecurityTag tag : tags) {
            int rank = taxonomy.getLevelRank(tag.getLevel());
            minRank = Math.min(minRank, rank);
            maxRank = Math.max(maxRank, rank);
        }
        return (maxRank - minRank) > 1;
    }

    private List<SecurityTag> convertNerEntities(List<NerEntity> entities) {
        List<SecurityTag> tags = new ArrayList<>();
        Map<String, String> labelMapping = taxonomy.getNerLabelMapping();
        
        for (NerEntity entity : entities) {
            String label = entity.getLabel();
            String category = label;
            String level = "L3"; // Default NER level

            if (labelMapping != null && labelMapping.containsKey(label)) {
                String mapped = labelMapping.get(label);
                String[] parts = mapped.split(":");
                if (parts.length >= 2) {
                    level = parts[0];
                    category = parts[1];
                } else {
                    category = mapped;
                }
            }

            tags.add(new SecurityTag(level, category, EngineLayer.NER, entity.getScore(), "ner:" + label));
        }

        return tags;
    }

    private String resolveLevel(List<SecurityTag> tags) {
        if (tags.isEmpty()) {
            return taxonomy.getDefaultLevel();
        }
        String[] levels = tags.stream().map(SecurityTag::getLevel).toArray(String[]::new);
        return taxonomy.maxLevel(levels);
    }

    /**
     * Result of funnel classification.
     */
    public static class FunnelResult {
        private final List<SecurityTag> tags;
        private final List<SecurityTag> suppressedTags;
        private final String finalLevel;
        private final double confidence;
        private final String engineLayer;
        private final boolean needsHumanReview;
        private final String reasoning;

        public FunnelResult(List<SecurityTag> tags, List<SecurityTag> suppressedTags, String finalLevel,
                           double confidence, String engineLayer, boolean needsHumanReview, String reasoning) {
            this.tags = tags;
            this.suppressedTags = suppressedTags;
            this.finalLevel = finalLevel;
            this.confidence = confidence;
            this.engineLayer = engineLayer;
            this.needsHumanReview = needsHumanReview;
            this.reasoning = reasoning;
        }

        public List<SecurityTag> getTags() { return tags; }
        public List<SecurityTag> getSuppressedTags() { return suppressedTags; }
        public String getFinalLevel() { return finalLevel; }
        public double getConfidence() { return confidence; }
        public String getEngineLayer() { return engineLayer; }
        public boolean isNeedsHumanReview() { return needsHumanReview; }
        public String getReasoning() { return reasoning; }
    }
}
