package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a composite rule for record-level multi-field combination upgrade.
 * 定义记录级多字段组合升级的复合规则。
 */
public class CompositeRuleDef {
    private String id;
    private String name;
    private String targetLevel;
    private String category;
    private int minMatches = 2;
    private List<String> fieldPatterns = new ArrayList<>();
    private String tableNamePattern;
    private String boostLevel;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetLevel() { return targetLevel; }
    public void setTargetLevel(String targetLevel) { this.targetLevel = targetLevel; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getMinMatches() { return minMatches; }
    public void setMinMatches(int minMatches) { this.minMatches = minMatches; }

    public List<String> getFieldPatterns() { return fieldPatterns; }
    public void setFieldPatterns(List<String> fieldPatterns) { this.fieldPatterns = fieldPatterns; }

    public String getTableNamePattern() { return tableNamePattern; }
    public void setTableNamePattern(String tableNamePattern) { this.tableNamePattern = tableNamePattern; }

    public String getBoostLevel() { return boostLevel; }
    public void setBoostLevel(String boostLevel) { this.boostLevel = boostLevel; }
}
