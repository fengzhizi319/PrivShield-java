package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a complete domain classification taxonomy.
 * 定义完整的领域分类体系。
 */
public class DomainTaxonomy {
    private String version = "1.0.0";
    private String defaultLevel = "L1";
    private List<SensitivityLevelDef> levels = new ArrayList<>();
    private List<CategoryDef> categories = new ArrayList<>();
    private ConfidencePolicy confidencePolicy;
    private String nerModelPath;
    private String nerVocabPath;
    private Map<String, String> nerLabelMapping;

    private transient Map<String, Integer> levelRankCache;
    private transient Map<String, String> categoryPathCache;

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getDefaultLevel() { return defaultLevel; }
    public void setDefaultLevel(String defaultLevel) { this.defaultLevel = defaultLevel; }

    public List<SensitivityLevelDef> getLevels() { return levels; }
    public void setLevels(List<SensitivityLevelDef> levels) { this.levels = levels; }

    public List<CategoryDef> getCategories() { return categories; }
    public void setCategories(List<CategoryDef> categories) { this.categories = categories; }

    public ConfidencePolicy getConfidencePolicy() { return confidencePolicy; }
    public void setConfidencePolicy(ConfidencePolicy confidencePolicy) { this.confidencePolicy = confidencePolicy; }

    public String getNerModelPath() { return nerModelPath; }
    public void setNerModelPath(String nerModelPath) { this.nerModelPath = nerModelPath; }

    public String getNerVocabPath() { return nerVocabPath; }
    public void setNerVocabPath(String nerVocabPath) { this.nerVocabPath = nerVocabPath; }

    public Map<String, String> getNerLabelMapping() { return nerLabelMapping; }
    public void setNerLabelMapping(Map<String, String> nerLabelMapping) { this.nerLabelMapping = nerLabelMapping; }

    /**
     * Returns the rank of a level ID. Returns 0 if not found.
     * 返回等级 ID 的排序值，未找到返回 0。
     */
    public int getLevelRank(String levelId) {
        if (levelRankCache == null) {
            buildCaches();
        }
        return levelRankCache.getOrDefault(levelId, 0);
    }

    /**
     * Returns the category path for a category ID.
     * 返回类别 ID 的完整路径。
     */
    public String getCategoryPath(String categoryId) {
        if (categoryPathCache == null) {
            buildCaches();
        }
        return categoryPathCache.getOrDefault(categoryId, categoryId);
    }

    /**
     * Returns the maximum level among the given level IDs.
     * 返回给定等级 ID 中的最高等级。
     */
    public String maxLevel(String... levelIds) {
        if (levelIds == null || levelIds.length == 0) {
            return defaultLevel;
        }
        String max = levelIds[0];
        int maxRank = getLevelRank(max);
        for (int i = 1; i < levelIds.length; i++) {
            int rank = getLevelRank(levelIds[i]);
            if (rank > maxRank) {
                maxRank = rank;
                max = levelIds[i];
            }
        }
        return max;
    }

    private synchronized void buildCaches() {
        levelRankCache = new HashMap<>();
        categoryPathCache = new HashMap<>();
        for (SensitivityLevelDef level : levels) {
            levelRankCache.put(level.getId(), level.getRank());
        }
        for (CategoryDef cat : categories) {
            categoryPathCache.put(cat.getId(), cat.getPath() != null ? cat.getPath() : cat.getId());
        }
    }

    /**
     * Creates a default built-in taxonomy for out-of-the-box usage.
     * 创建内置的默认分类体系，用于开箱即用。
     */
    public static DomainTaxonomy createDefault() {
        DomainTaxonomy t = new DomainTaxonomy();
        t.setVersion("1.0.0");
        t.setDefaultLevel("L1");

        List<SensitivityLevelDef> levels = new ArrayList<>();
        levels.add(createLevel("L1", "公开", 1, "Public data"));
        levels.add(createLevel("L2", "内部", 2, "Internal data"));
        levels.add(createLevel("L3", "敏感", 3, "Sensitive personal data"));
        levels.add(createLevel("L4", "高敏感", 4, "Highly sensitive data"));
        levels.add(createLevel("L5", "极敏感", 5, "Extremely sensitive data"));
        t.setLevels(levels);

        List<CategoryDef> categories = new ArrayList<>();
        categories.add(createCategory("personal-info", "个人信息", "general/personal-info"));
        categories.add(createCategory("contact", "联系方式", "general/contact"));
        categories.add(createCategory("identity", "身份信息", "general/identity"));
        categories.add(createCategory("financial", "金融信息", "finance/financial"));
        categories.add(createCategory("medical", "医疗信息", "health/medical"));
        t.setCategories(categories);

        return t;
    }

    private static SensitivityLevelDef createLevel(String id, String name, int rank, String desc) {
        SensitivityLevelDef l = new SensitivityLevelDef();
        l.setId(id);
        l.setName(name);
        l.setRank(rank);
        l.setDescription(desc);
        return l;
    }

    private static CategoryDef createCategory(String id, String name, String path) {
        CategoryDef c = new CategoryDef();
        c.setId(id);
        c.setName(name);
        c.setPath(path);
        return c;
    }
}
