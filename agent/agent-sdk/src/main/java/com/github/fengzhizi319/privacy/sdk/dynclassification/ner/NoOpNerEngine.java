package com.github.fengzhizi319.privacy.sdk.dynclassification.ner;

import java.util.ArrayList;
import java.util.List;

/**
 * No-op NER engine that returns empty results.
 * 返回空结果的空操作 NER 引擎。
 */
public class NoOpNerEngine implements NerEngine {
    @Override
    public List<NerEntity> extract(String fieldName, String text) {
        return new ArrayList<>();
    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
