package com.github.fengzhizi319.privacy.sdk.dynclassification.ner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lazy-loading adapter for NER engines.
 * NER 引擎的延迟加载适配器。
 */
public class NerAdapter {
    private static final Logger log = LoggerFactory.getLogger(NerAdapter.class);

    private final String modelPath;
    private final String vocabPath;
    private final Map<String, String> labelMapping;
    private final AtomicReference<NerEngine> engineRef = new AtomicReference<>();
    private volatile boolean initialized = false;

    public NerAdapter(String modelPath, String vocabPath, Map<String, String> labelMapping) {
        this.modelPath = modelPath;
        this.vocabPath = vocabPath;
        this.labelMapping = labelMapping;
    }

    /**
     * Extracts entities using the NER engine (lazy initialization).
     * 使用 NER 引擎提取实体（延迟初始化）。
     */
    public List<NerEntity> extract(String fieldName, String text) {
        NerEngine engine = getEngine();
        if (engine == null || !engine.isAvailable()) {
            return new ArrayList<>();
        }
        try {
            return engine.extract(fieldName, text);
        } catch (Exception e) {
            log.warn("NER extraction failed: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private NerEngine getEngine() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    engineRef.set(createEngine());
                    initialized = true;
                }
            }
        }
        return engineRef.get();
    }

    private NerEngine createEngine() {
        if (modelPath == null || modelPath.isEmpty()) {
            log.debug("No NER model path configured, using NoOp engine");
            return new NoOpNerEngine();
        }

        // Try to create ONNX engine
        try {
            OnnxNerEngine engine = new OnnxNerEngine(modelPath, vocabPath, labelMapping);
            if (engine.isAvailable()) {
                log.info("ONNX NER engine initialized successfully");
                return engine;
            }
        } catch (Exception e) {
            log.warn("Failed to initialize ONNX NER engine: {}", e.getMessage());
        }

        return new NoOpNerEngine();
    }

    /**
     * Resets the adapter for reload.
     * 重置适配器以便重新加载。
     */
    public void reset() {
        synchronized (this) {
            initialized = false;
            engineRef.set(null);
        }
    }
}
