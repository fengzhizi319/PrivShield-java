package com.github.fengzhizi319.privacy.sdk.dynclassification.llm;

import com.github.fengzhizi319.privacy.sdk.dynclassification.model.DomainTaxonomy;

/**
 * No-op LLM classifier that returns null.
 * 返回 null 的空操作 LLM 分类器。
 */
public class NoOpLlmClassifier implements LlmClassifier {
    @Override
    public String classify(String fieldName, String fieldValue, DomainTaxonomy taxonomy) {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
