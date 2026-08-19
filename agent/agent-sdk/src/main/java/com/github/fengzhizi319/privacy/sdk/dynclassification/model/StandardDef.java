package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a standard combination of multiple domain packs.
 * 定义多个领域包的标准组合。
 */
public class StandardDef {
    private String id;
    private String name;
    private String taxonomy = "default";
    private List<String> domains = new ArrayList<>();
    private Map<String, Object> ruleOverrides = new HashMap<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTaxonomy() { return taxonomy; }
    public void setTaxonomy(String taxonomy) { this.taxonomy = taxonomy; }

    public List<String> getDomains() { return domains; }
    public void setDomains(List<String> domains) { this.domains = domains; }

    public Map<String, Object> getRuleOverrides() { return ruleOverrides; }
    public void setRuleOverrides(Map<String, Object> ruleOverrides) { this.ruleOverrides = ruleOverrides; }
}
