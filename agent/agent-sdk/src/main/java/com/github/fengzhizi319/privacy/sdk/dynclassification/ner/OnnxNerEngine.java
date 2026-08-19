package com.github.fengzhizi319.privacy.sdk.dynclassification.ner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ONNX Runtime based NER engine (optional dependency).
 * This is a stub implementation that checks for ONNX Runtime availability.
 * 基于 ONNX Runtime 的 NER 引擎（可选依赖），检查运行时可用性。
 */
public class OnnxNerEngine implements NerEngine {
    private static final Logger log = LoggerFactory.getLogger(OnnxNerEngine.class);

    private final String modelPath;
    private final String vocabPath;
    private final Map<String, String> labelMapping;
    private final boolean available;

    public OnnxNerEngine(String modelPath, String vocabPath, Map<String, String> labelMapping) {
        this.modelPath = modelPath;
        this.vocabPath = vocabPath;
        this.labelMapping = labelMapping;
        this.available = checkAvailability();
    }

    private boolean checkAvailability() {
        // Check if model file exists
        if (modelPath == null || modelPath.isEmpty()) {
            return false;
        }
        File modelFile = new File(modelPath);
        if (!modelFile.exists() || !modelFile.isFile()) {
            log.debug("ONNX model file not found: {}", modelPath);
            return false;
        }

        // Check if ONNX Runtime is available
        try {
            Class.forName("ai.onnxruntime.OrtEnvironment");
            return true;
        } catch (ClassNotFoundException e) {
            log.debug("ONNX Runtime not available on classpath");
            return false;
        }
    }

    @Override
    public List<NerEntity> extract(String fieldName, String text) {
        if (!available) {
            return new ArrayList<>();
        }
        // TODO: Implement actual ONNX inference when onnxruntime dependency is added
        // This requires: BertTokenizer, ONNX session, BIO tag parsing
        log.debug("ONNX NER extraction not yet implemented");
        return new ArrayList<>();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
