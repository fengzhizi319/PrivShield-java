package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

/**
 * Defines a dynamic sensitivity level.
 * 定义动态敏感等级。
 */
public class SensitivityLevelDef {
    private String id;
    private String name;
    private int rank;
    private String description;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
