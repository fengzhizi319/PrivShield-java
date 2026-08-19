package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a classification rule.
 * 定义分类规则。
 */
public class RuleDef {
    private String id;
    private String name;
    private String level;
    private String category;
    private int priority = 50;
    private List<MatcherDef> matchers = new ArrayList<>();
    private String matchLogic = "OR";
    private boolean enabled = true;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public List<MatcherDef> getMatchers() { return matchers; }
    public void setMatchers(List<MatcherDef> matchers) { this.matchers = matchers; }

    public String getMatchLogic() { return matchLogic; }
    public void setMatchLogic(String matchLogic) { this.matchLogic = matchLogic; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
