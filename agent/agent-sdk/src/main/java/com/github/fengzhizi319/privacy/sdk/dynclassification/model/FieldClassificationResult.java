package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classification result for a single field.
 * 单个字段的分类结果。
 */
public class FieldClassificationResult {
    private String fieldName;
    private String fieldValue;
    private List<SecurityTag> tags = new ArrayList<>();
    private String finalLevel;
    private double confidence;
    private boolean needsHumanReview;
    private String engineLayer;
    private String reasoning;
    private List<SecurityTag> suppressedTags = new ArrayList<>();

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }

    public List<SecurityTag> getTags() { return tags; }
    public void setTags(List<SecurityTag> tags) { this.tags = tags; }

    public String getFinalLevel() { return finalLevel; }
    public void setFinalLevel(String finalLevel) { this.finalLevel = finalLevel; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public void setNeedsHumanReview(boolean needsHumanReview) { this.needsHumanReview = needsHumanReview; }

    public String getEngineLayer() { return engineLayer; }
    public void setEngineLayer(String engineLayer) { this.engineLayer = engineLayer; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public List<SecurityTag> getSuppressedTags() { return suppressedTags; }
    public void setSuppressedTags(List<SecurityTag> suppressedTags) { this.suppressedTags = suppressedTags; }
}
