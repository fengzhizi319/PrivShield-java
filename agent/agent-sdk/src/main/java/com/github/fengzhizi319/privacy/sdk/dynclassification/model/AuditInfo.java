package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.time.Instant;

/**
 * Audit information for classification operations.
 * 分类操作的审计信息。
 */
public class AuditInfo {
    private String version = "1.0.0";
    private String domain;
    private String standardId;
    private String timestamp;
    private String ruleSetVersion = "1.0.0";

    public AuditInfo() {
        this.timestamp = Instant.now().toString();
    }

    public AuditInfo(String domain, String standardId) {
        this();
        this.domain = domain;
        this.standardId = standardId;
    }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getStandardId() { return standardId; }
    public void setStandardId(String standardId) { this.standardId = standardId; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getRuleSetVersion() { return ruleSetVersion; }
    public void setRuleSetVersion(String ruleSetVersion) { this.ruleSetVersion = ruleSetVersion; }
}
