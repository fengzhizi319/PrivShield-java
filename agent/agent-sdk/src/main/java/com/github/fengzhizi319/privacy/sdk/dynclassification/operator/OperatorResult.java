package com.github.fengzhizi319.privacy.sdk.dynclassification.operator;

/**
 * Result of an operator evaluation.
 * 算子评估的结果。
 */
public class OperatorResult {
    private final boolean hit;
    private final String level;
    private final String category;

    private OperatorResult(boolean hit, String level, String category) {
        this.hit = hit;
        this.level = level;
        this.category = category;
    }

    /**
     * Creates a miss result.
     * 创建未命中结果。
     */
    public static OperatorResult miss() {
        return new OperatorResult(false, null, null);
    }

    /**
     * Creates a hit result with level and category.
     * 创建带等级和类别的命中结果。
     */
    public static OperatorResult hit(String level, String category) {
        return new OperatorResult(true, level, category);
    }

    /**
     * Creates a hit result without level and category.
     * 创建不带等级和类别的命中结果。
     */
    public static OperatorResult hit() {
        return new OperatorResult(true, null, null);
    }

    public boolean isHit() { return hit; }
    public String getLevel() { return level; }
    public String getCategory() { return category; }
}
