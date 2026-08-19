package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

/**
 * Confidence policy controlling decay and LLM arbitration triggers.
 * 控制置信度衰减和 LLM 仲裁触发的策略。
 */
public class ConfidencePolicy {
    private double ruleConfidence = 0.95;
    private double nerConfidence = 0.80;
    private double llmConfidence = 0.70;
    private double conflictThreshold = 0.15;
    private boolean enableNer = false;
    private boolean enableLlm = false;
    private double llmTriggerThreshold = 0.60;

    public double getRuleConfidence() { return ruleConfidence; }
    public void setRuleConfidence(double v) { this.ruleConfidence = v; }

    public double getNerConfidence() { return nerConfidence; }
    public void setNerConfidence(double v) { this.nerConfidence = v; }

    public double getLlmConfidence() { return llmConfidence; }
    public void setLlmConfidence(double v) { this.llmConfidence = v; }

    public double getConflictThreshold() { return conflictThreshold; }
    public void setConflictThreshold(double v) { this.conflictThreshold = v; }

    public boolean isEnableNer() { return enableNer; }
    public void setEnableNer(boolean v) { this.enableNer = v; }

    public boolean isEnableLlm() { return enableLlm; }
    public void setEnableLlm(boolean v) { this.enableLlm = v; }

    public double getLlmTriggerThreshold() { return llmTriggerThreshold; }
    public void setLlmTriggerThreshold(double v) { this.llmTriggerThreshold = v; }
}
