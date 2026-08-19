package com.github.fengzhizi319.privacy.sdk.api;

import com.github.fengzhizi319.privacy.sdk.dynclassification.DynClassificationService;
import com.github.fengzhizi319.privacy.sdk.dynclassification.DynClassificationService.ServiceConfig;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.FieldClassificationResult;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.RecordClassificationResult;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.TableClassificationResult;

import java.util.List;
import java.util.Map;

/**
 * Public API for dynamic data classification.
 */
public class DynClassificationApi {
    private final DynClassificationService service;

    /**
     * Creates a new DynClassificationApi with default configuration.
     */
    public DynClassificationApi() {
        this(new ServiceConfig());
    }

    /**
     * Creates a new DynClassificationApi with the given rules directory.
     */
    public DynClassificationApi(String rulesDir) {
        ServiceConfig config = new ServiceConfig();
        config.setRulesDir(rulesDir);
        this.service = new DynClassificationService(config);
    }

    /**
     * Creates a new DynClassificationApi with the given configuration.
     */
    public DynClassificationApi(ServiceConfig config) {
        this.service = new DynClassificationService(config);
    }

    /**
     * Classifies a single field.
     */
    public FieldClassificationResult classifyField(String fieldName, String value, String domain, String standard) {
        return service.classifyField(fieldName, value, domain, standard);
    }

    /**
     * Classifies a single field with default domain/standard.
     */
    public FieldClassificationResult classifyField(String fieldName, String value) {
        return service.classifyField(fieldName, value, null, null);
    }

    /**
     * Classifies a record (multiple fields).
     */
    public RecordClassificationResult classifyRecord(Map<String, String> record, String domain, String standard) {
        return service.classifyRecord(record, domain, standard);
    }

    /**
     * Classifies a record with default domain/standard.
     */
    public RecordClassificationResult classifyRecord(Map<String, String> record) {
        return service.classifyRecord(record, null, null);
    }

    /**
     * Classifies an entire table.
     */
    public TableClassificationResult classifyTable(List<Map<String, String>> records, List<String> schema,
                                                    String domain, String standard) {
        return service.classifyTable(records, schema, domain, standard);
    }

    /**
     * Classifies an entire table with default domain/standard.
     */
    public TableClassificationResult classifyTable(List<Map<String, String>> records, List<String> schema) {
        return service.classifyTable(records, schema, null, null);
    }

    /**
     * Performs a classification preview without side effects.
     */
    public Map<String, FieldClassificationResult> dryRun(Map<String, String> fields, String domain, String standard) {
        return service.dryRun(fields, domain, standard);
    }

    /**
     * Clears all caches and forces re-loading of rules/models on next use.
     */
    public void reload() {
        service.reload();
    }

    /**
     * Lists available standard names.
     */
    public List<String> listStandards() {
        return service.listStandards();
    }

    /**
     * Lists available domain names.
     */
    public List<String> listDomains() {
        return service.listDomains();
    }

    /**
     * Lists registered operator names.
     */
    public List<String> listOperators() {
        return service.listOperators();
    }

    /**
     * Returns the underlying service for advanced usage.
     */
    public DynClassificationService getService() {
        return service;
    }
}
