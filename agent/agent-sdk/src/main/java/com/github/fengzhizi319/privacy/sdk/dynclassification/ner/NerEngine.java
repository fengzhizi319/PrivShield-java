package com.github.fengzhizi319.privacy.sdk.dynclassification.ner;

import java.util.List;

/**
 * Interface for NER engines.
 * NER 引擎的接口。
 */
public interface NerEngine {
    /**
     * Extracts named entities from text.
     * 从文本中提取命名实体。
     */
    List<NerEntity> extract(String fieldName, String text);

    /**
     * Checks if the engine is available.
     * 检查引擎是否可用。
     */
    boolean isAvailable();
}
