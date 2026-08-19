package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a downgrade/override rule for suppressing tags.
 * 定义用于抑制标签的降级/覆盖规则。
 */
public class DowngradeRuleDef {
    private String id;
    private String name;
    private String level;
    private String category;
    private boolean forceSuppress = false;
    private String maxForceSuppressLevel;
    private List<String> exemptRules = new ArrayList<>();
    private List<String> keywords = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isForceSuppress() { return forceSuppress; }
    public void setForceSuppress(boolean forceSuppress) { this.forceSuppress = forceSuppress; }

    public String getMaxForceSuppressLevel() { return maxForceSuppressLevel; }
    public void setMaxForceSuppressLevel(String maxForceSuppressLevel) { this.maxForceSuppressLevel = maxForceSuppressLevel; }

    public List<String> getExemptRules() { return exemptRules; }
    public void setExemptRules(List<String> exemptRules) { this.exemptRules = exemptRules; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
}
