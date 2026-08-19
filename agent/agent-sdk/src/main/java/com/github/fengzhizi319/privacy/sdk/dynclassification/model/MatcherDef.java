package com.github.fengzhizi319.privacy.sdk.dynclassification.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a matcher within a rule.
 * 定义规则中的匹配器。
 */
public class MatcherDef {
    private String target;
    private String operator;
    private Map<String, Object> params = new HashMap<>();

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
}
