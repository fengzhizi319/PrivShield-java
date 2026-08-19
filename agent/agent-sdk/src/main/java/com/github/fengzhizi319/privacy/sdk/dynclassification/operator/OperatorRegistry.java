package com.github.fengzhizi319.privacy.sdk.dynclassification.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for matcher operators.
 * 线程安全的匹配算子注册表。
 */
public class OperatorRegistry {
    private final Map<String, MatcherOperator> operators = new ConcurrentHashMap<>();

    /**
     * Registers an operator with the given name.
     * 注册指定名称的算子。
     */
    public void register(String name, MatcherOperator operator) {
        operators.put(name, operator);
    }

    /**
     * Gets an operator by name.
     * 按名称获取算子。
     */
    public MatcherOperator get(String name) {
        return operators.get(name);
    }

    /**
     * Checks if an operator is registered.
     * 检查算子是否已注册。
     */
    public boolean has(String name) {
        return operators.containsKey(name);
    }

    /**
     * Returns all registered operator names.
     * 返回所有已注册的算子名称。
     */
    public List<String> listOperators() {
        return new ArrayList<>(operators.keySet());
    }

    /**
     * The default global registry with built-in operators.
     * 包含所有内置算子的默认全局注册表。
     */
    public static final OperatorRegistry DEFAULT = createDefault();

    private static OperatorRegistry createDefault() {
        OperatorRegistry registry = new OperatorRegistry();
        Operators.registerAll(registry);
        return registry;
    }
}
