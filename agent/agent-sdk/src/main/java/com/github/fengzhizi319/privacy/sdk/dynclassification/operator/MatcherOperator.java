package com.github.fengzhizi319.privacy.sdk.dynclassification.operator;

import java.util.Map;

/**
 * Functional interface for matcher operators.
 * 匹配算子的函数式接口。
 */
@FunctionalInterface
public interface MatcherOperator {
    /**
     * Evaluates the operator against the given field context.
     * 对给定字段上下文评估算子。
     *
     * @param fieldName  the name of the field being evaluated
     * @param fieldValue the value of the field
     * @param params     operator-specific parameters
     * @return the result of the evaluation
     */
    OperatorResult evaluate(String fieldName, String fieldValue, Map<String, Object> params);
}
