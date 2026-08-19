package com.github.fengzhizi319.privacy.sdk.dynclassification.engine;

import com.github.fengzhizi319.privacy.sdk.dynclassification.model.*;
import com.github.fengzhizi319.privacy.sdk.dynclassification.operator.MatcherOperator;
import com.github.fengzhizi319.privacy.sdk.dynclassification.operator.OperatorRegistry;
import com.github.fengzhizi319.privacy.sdk.dynclassification.operator.OperatorResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Configurable rule engine that evaluates classification rules against field values.
 * 可配置规则引擎，基于声明式规则对字段值进行分类评估。
 */
public class ConfigurableRuleEngine {
    private final DomainTaxonomy taxonomy;
    private final List<RuleDef> rules;
    private final List<DowngradeRuleDef> downgradeRules;
    private final String domain;
    private final String standardId;
    private final OperatorRegistry registry;

    public ConfigurableRuleEngine(DomainTaxonomy taxonomy, List<RuleProfile> profiles,
                                   String domain, String standardId) {
        this(taxonomy, profiles, domain, standardId, OperatorRegistry.DEFAULT);
    }

    public ConfigurableRuleEngine(DomainTaxonomy taxonomy, List<RuleProfile> profiles,
                                   String domain, String standardId, OperatorRegistry registry) {
        this.taxonomy = taxonomy;
        this.domain = domain;
        this.standardId = standardId;
        this.registry = registry;
        this.rules = new ArrayList<>();
        this.downgradeRules = new ArrayList<>();

        for (RuleProfile profile : profiles) {
            rules.addAll(profile.getRules());
            downgradeRules.addAll(profile.getDowngradeRules());
        }

        // Sort by priority descending
        rules.sort(Comparator.comparingInt(RuleDef::getPriority).reversed());
    }

    public DomainTaxonomy getTaxonomy() {
        return taxonomy;
    }

    /**
     * Evaluates all rules against a field and returns matching tags plus suppressed tags.
     */
    public EvaluateResult evaluate(String fieldName, String fieldValue) {
        List<SecurityTag> tags = new ArrayList<>();
        List<String> matchedRuleIds = new ArrayList<>();

        // Phase 1: Evaluate all rules
        for (RuleDef rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }
            if (evaluateRule(rule, fieldName, fieldValue)) {
                String level = rule.getLevel() != null ? rule.getLevel() : taxonomy.getDefaultLevel();
                String category = rule.getCategory() != null ? rule.getCategory() : "unknown";
                tags.add(new SecurityTag(level, category, EngineLayer.RULE, 0.95, rule.getId()));
                matchedRuleIds.add(rule.getId());
            }
        }

        // Phase 2: Apply downgrade/override rules
        List<SecurityTag> suppressedTags = applyOverrideSuppression(tags, matchedRuleIds, fieldName, fieldValue);

        // Remove duplicates
        tags = uniqueTags(tags);

        return new EvaluateResult(tags, suppressedTags);
    }

    private boolean evaluateRule(RuleDef rule, String fieldName, String fieldValue) {
        List<MatcherDef> matchers = rule.getMatchers();
        if (matchers == null || matchers.isEmpty()) {
            return false;
        }

        boolean isAnd = "AND".equalsIgnoreCase(rule.getMatchLogic());
        
        for (MatcherDef matcher : matchers) {
            boolean hit = executeMatcher(matcher, fieldName, fieldValue);
            if (isAnd && !hit) {
                return false;
            }
            if (!isAnd && hit) {
                return true;
            }
        }

        return isAnd; // AND: all passed; OR: none hit
    }

    private boolean executeMatcher(MatcherDef matcher, String fieldName, String fieldValue) {
        MatcherOperator op = registry.get(matcher.getOperator());
        if (op == null) {
            return false;
        }
        OperatorResult result = op.evaluate(fieldName, fieldValue, matcher.getParams());
        return result.isHit();
    }

    private List<SecurityTag> applyOverrideSuppression(List<SecurityTag> tags, List<String> matchedRuleIds,
                                                        String fieldName, String fieldValue) {
        List<SecurityTag> suppressed = new ArrayList<>();
        
        for (DowngradeRuleDef dr : downgradeRules) {
            if (!dr.isForceSuppress()) {
                continue;
            }

            // Check if any keyword matches
            boolean keywordMatch = dr.getKeywords().isEmpty();
            if (!keywordMatch) {
                String normalizedValue = normalizeKeyword(fieldValue != null ? fieldValue : "");
                String normalizedName = normalizeKeyword(fieldName != null ? fieldName : "");
                for (String kw : dr.getKeywords()) {
                    String normKw = normalizeKeyword(kw);
                    if (normalizedValue.contains(normKw) || normalizedName.contains(normKw)) {
                        keywordMatch = true;
                        break;
                    }
                }
            }

            if (!keywordMatch) {
                continue;
            }

            // Get max suppress level rank
            int maxSuppressRank = Integer.MAX_VALUE;
            if (dr.getMaxForceSuppressLevel() != null) {
                maxSuppressRank = taxonomy.getLevelRank(dr.getMaxForceSuppressLevel());
            }

            // Suppress tags
            List<SecurityTag> toRemove = new ArrayList<>();
            for (SecurityTag tag : tags) {
                // Check if rule is exempt
                boolean exempt = false;
                for (String pattern : dr.getExemptRules()) {
                    if (matchesGlob(pattern, tag.getRuleId())) {
                        exempt = true;
                        break;
                    }
                }
                if (exempt) {
                    continue;
                }

                int tagRank = taxonomy.getLevelRank(tag.getLevel());
                if (tagRank > maxSuppressRank) {
                    // Tag exceeds the allowed cap → suppress it
                    // 标签超出允许上限 → 抑制
                    toRemove.add(tag);
                    suppressed.add(tag);
                }
            }
            tags.removeAll(toRemove);
        }

        return suppressed;
    }

    /**
     * Matches a glob pattern against a value (supports * and ?).
     * 匹配 glob 模式（支持 * 和 ?）。
     * Security: properly escapes regex special chars to prevent injection.
     * 安全：正确转义正则特殊字符以防止注入。
     */
    private boolean matchesGlob(String pattern, String value) {
        if (pattern == null || value == null) {
            return false;
        }
        // Convert glob to regex with proper escaping
        // 将 glob 转换为正则并正确转义
        StringBuilder regex = new StringBuilder();
        for (char c : pattern.toCharArray()) {
            switch (c) {
                case '*': regex.append(".*"); break;
                case '?': regex.append('.'); break;
                case '.': case '\\': case '[': case ']':
                case '(': case ')': case '{': case '}':
                case '^': case '$': case '|': case '+':
                    regex.append('\\').append(c); break;
                default: regex.append(c);
            }
        }
        try {
            return Pattern.matches(regex.toString(), value);
        } catch (Exception e) {
            return false;
        }
    }

    private List<SecurityTag> uniqueTags(List<SecurityTag> tags) {
        List<SecurityTag> unique = new ArrayList<>();
        for (SecurityTag tag : tags) {
            boolean exists = false;
            for (SecurityTag u : unique) {
                if (u.getLevel().equals(tag.getLevel()) && 
                    u.getCategory().equals(tag.getCategory()) &&
                    u.getRuleId().equals(tag.getRuleId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                unique.add(tag);
            }
        }
        return unique;
    }

    private String normalizeKeyword(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[_\\-\\s]+", "");
    }

    /**
     * Result of rule evaluation.
     */
    public static class EvaluateResult {
        private final List<SecurityTag> tags;
        private final List<SecurityTag> suppressedTags;

        public EvaluateResult(List<SecurityTag> tags, List<SecurityTag> suppressedTags) {
            this.tags = tags;
            this.suppressedTags = suppressedTags;
        }

        public List<SecurityTag> getTags() { return tags; }
        public List<SecurityTag> getSuppressedTags() { return suppressedTags; }
    }
}
