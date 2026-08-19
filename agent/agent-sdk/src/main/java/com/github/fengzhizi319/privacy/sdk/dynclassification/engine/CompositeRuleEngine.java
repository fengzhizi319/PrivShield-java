package com.github.fengzhizi319.privacy.sdk.dynclassification.engine;

import com.github.fengzhizi319.privacy.sdk.dynclassification.model.CompositeRuleDef;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.DomainTaxonomy;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.EngineLayer;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.SecurityTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Composite rule engine for record-level multi-field combination upgrade.
 * 复合规则引擎，用于记录级多字段组合升级。
 */
public class CompositeRuleEngine {
    private final List<CompositeRuleDef> rules;
    private final String domain;
    private final String standardId;
    private final Map<String, List<Pattern>> compiledPatterns;

    public CompositeRuleEngine(List<CompositeRuleDef> rules, String domain, String standardId) {
        this.rules = rules != null ? rules : new ArrayList<>();
        this.domain = domain;
        this.standardId = standardId;
        this.compiledPatterns = new HashMap<>();

        // Pre-compile patterns
        for (CompositeRuleDef rule : this.rules) {
            List<Pattern> patterns = new ArrayList<>();
            for (String p : rule.getFieldPatterns()) {
                try {
                    String bounded = "(?i)\\b" + p + "\\b";
                    patterns.add(Pattern.compile(bounded));
                } catch (Exception e) {
                    // Skip invalid patterns
                }
            }
            compiledPatterns.put(rule.getId(), patterns);
        }
    }

    /**
     * Evaluates composite rules against a record.
     * 对记录评估复合规则。
     */
    public List<SecurityTag> evaluate(Map<String, String> record) {
        List<SecurityTag> tags = new ArrayList<>();
        
        // Normalize field names
        Map<String, String> normFields = new HashMap<>();
        for (String name : record.keySet()) {
            normFields.put(name.toLowerCase(Locale.ROOT), name);
        }

        for (CompositeRuleDef rule : rules) {
            List<Pattern> patterns = compiledPatterns.get(rule.getId());
            if (patterns == null || patterns.isEmpty()) {
                continue;
            }

            int matched = 0;
            for (Pattern pattern : patterns) {
                for (String normName : normFields.keySet()) {
                    if (pattern.matcher(normName).find()) {
                        matched++;
                        break;
                    }
                }
            }

            if (matched >= rule.getMinMatches()) {
                tags.add(new SecurityTag(
                    rule.getTargetLevel(),
                    rule.getCategory(),
                    EngineLayer.RULE,
                    0.90,
                    rule.getId()
                ));
            }
        }

        return tags;
    }

    /**
     * Applies composite rule results to record level.
     * 将复合规则结果应用到记录级别。
     */
    public String applyToRecordLevel(String currentLevel, List<SecurityTag> compositeTags, DomainTaxonomy taxonomy) {
        if (compositeTags == null || compositeTags.isEmpty()) {
            return currentLevel;
        }

        String maxLevel = currentLevel;
        int maxRank = currentLevel != null ? taxonomy.getLevelRank(currentLevel) : 0;

        for (SecurityTag tag : compositeTags) {
            int rank = taxonomy.getLevelRank(tag.getLevel());
            if (rank > maxRank) {
                maxRank = rank;
                maxLevel = tag.getLevel();
            }
        }

        return maxLevel;
    }
}
