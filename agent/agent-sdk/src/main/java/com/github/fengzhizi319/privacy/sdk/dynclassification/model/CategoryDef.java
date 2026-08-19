package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

/**
 * Defines a dynamic classification category.
 * 定义动态分类类别。
 */
public class CategoryDef {
    private String id;
    private String name;
    private String path;
    private String description;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
