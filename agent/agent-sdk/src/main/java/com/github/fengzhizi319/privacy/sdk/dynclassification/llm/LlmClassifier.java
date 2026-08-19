package com.github.fengzhizi319.privacy.sdk.dynclassification.llm;

import com.github.fengzhizi319.privacy.sdk.dynclassification.model.DomainTaxonomy;

/**
 * Interface for LLM classifiers.
 * LLM 分类器的接口。
 */
public interface LlmClassifier {
    /**
     * Classifies a field value and returns the sensitivity level.
     * 对字段值进行分类并返回敏感等级。
     */
    String classify(String fieldName, String fieldValue, DomainTaxonomy taxonomy);

    /**
     * Checks if the classifier is available.
     * 检查分类器是否可用。
     */
    boolean isAvailable();
}
