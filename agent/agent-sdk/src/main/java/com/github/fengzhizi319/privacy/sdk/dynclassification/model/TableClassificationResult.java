package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Classification result for an entire table (multiple records).
 * 整张表（多条记录）的分类结果。
 */
public class TableClassificationResult {
    private List<String> schema = new ArrayList<>();
    private List<RecordClassificationResult> recordResults = new ArrayList<>();
    private List<SecurityTag> aggregatedTags = new ArrayList<>();
    private String finalLevel;
    private double confidence;
    private boolean needsHumanReview;

    public List<String> getSchema() { return schema; }
    public void setSchema(List<String> schema) { this.schema = schema; }

    public List<RecordClassificationResult> getRecordResults() { return recordResults; }
    public void setRecordResults(List<RecordClassificationResult> recordResults) { this.recordResults = recordResults; }

    public List<SecurityTag> getAggregatedTags() { return aggregatedTags; }
    public void setAggregatedTags(List<SecurityTag> aggregatedTags) { this.aggregatedTags = aggregatedTags; }

    public String getFinalLevel() { return finalLevel; }
    public void setFinalLevel(String finalLevel) { this.finalLevel = finalLevel; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isNeedsHumanReview() { return needsHumanReview; }
    public void setNeedsHumanReview(boolean needsHumanReview) { this.needsHumanReview = needsHumanReview; }
}
