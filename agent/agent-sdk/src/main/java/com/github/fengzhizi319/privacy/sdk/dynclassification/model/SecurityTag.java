package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

/**
 * Represents a single security/sensitivity tag.
 * 表示单个安全/敏感标签。
 */
public class SecurityTag {
    private String level;
    private String category;
    private String source;
    private double confidence;
    private String ruleId;

    public SecurityTag() {}

    public SecurityTag(String level, String category, String source, double confidence, String ruleId) {
        this.level = level;
        this.category = category;
        this.source = source;
        this.confidence = confidence;
        this.ruleId = ruleId;
    }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    @Override
    public String toString() {
        return "SecurityTag{level='" + level + "', category='" + category + "', source='" + source + "'}";
    }
}
