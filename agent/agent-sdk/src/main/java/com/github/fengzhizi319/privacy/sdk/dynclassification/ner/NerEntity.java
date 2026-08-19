package com.github.fengzhizi319.privacy.sdk.dynclassification.ner;

/**
 * Represents a named entity extracted by NER.
 * 表示 NER 提取的命名实体。
 */
public class NerEntity {
    private final String text;
    private final String label;
    private final double score;
    private final int start;
    private final int end;

    public NerEntity(String text, String label, double score, int start, int end) {
        this.text = text;
        this.label = label;
        this.score = score;
        this.start = start;
        this.end = end;
    }

    public String getText() { return text; }
    public String getLabel() { return label; }
    public double getScore() { return score; }
    public int getStart() { return start; }
    public int getEnd() { return end; }

    @Override
    public String toString() {
        return "NerEntity{text='" + text + "', label='" + label + "', score=" + score + "}";
    }
}
