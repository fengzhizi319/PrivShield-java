package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classification result for a single record (multiple fields).
 * 单条记录（多个字段）的分类结果。
 */
public class RecordClassificationResult {
    private int recordIndex;
    private Map<String, FieldClassificationResult> fieldResults = new HashMap<>();
    private List<SecurityTag> aggregatedTags = new ArrayList<>();
    private String finalLevel;
    private double confidence;
    private boolean needsHumanReview;

    public int getRecordIndex() { return recordIndex; }
    public void setRecordIndex(int recordIndex) { this.recordIndex = recordIndex; }

    public Map<String, FieldClassificationResult> getFieldResults() { return fieldResults; }
    public void setFieldResults(Map<String, FieldClassificationResult> fieldResults) { this.fieldResults = fieldResults; }

    public List<SecurityTag> getAggregatedTags() { return aggregatedTags; }
    public void setAggregatedTags(List<SecurityTag> aggregatedTags) { this.aggregatedTags = aggregatedTags; }

    public String getFinalLevel() { return finalLevel; }
    public void setFinalLevel(String finalLevel) { this.finalLevel = finalLevel; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public void setNeedsHumanReview(boolean needsHumanReview) { this.needsHumanReview = needsHumanReview; }
}
