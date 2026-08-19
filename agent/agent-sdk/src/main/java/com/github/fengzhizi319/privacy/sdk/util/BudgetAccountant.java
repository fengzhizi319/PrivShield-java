package com.github.fengzhizi319.privacy.sdk.util;

import com.github.fengzhizi319.privacy.sdk.exception.PrivacyBudgetExhaustedException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 隐私预算记账本（Privacy Budget Accountant）。
 * <p>
 * 按 namespace 维护 epsilon 与 delta 的总预算与已消耗量，所有 spend 操作线程安全。
 * 每个 namespace 在 JVM 生命周期内仅存在一个实例，便于跨调用共享预算状态。
 * </p>
 *
 * <p><b>实现注意点：</b></p>
 * <ul>
 *   <li>使用 {@link ConcurrentHashMap} 与同步方法保证并发安全，但高并发场景下仍是进程内单点。</li>
 *   <li>预算耗尽后会抛出 {@link PrivacyBudgetExhaustedException}，调用方应捕获并决定是否阻断后续计算。</li>
 * </ul>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class BudgetAccountant {

    /** 预算所属的命名空间。 */
    private final String namespace;

    /** epsilon 总预算。 */
    private final double epsilonTotal;

    /** delta 总预算。 */
    private final double deltaTotal;

    /** 已消耗的 epsilon。 */
    private double epsilonSpent = 0.0;

    /** 已消耗的 delta。 */
    private double deltaSpent = 0.0;

    /** 按 namespace 缓存的单例实例映射。 */
    private static final Map<String, BudgetAccountant> INSTANCES = new ConcurrentHashMap<>();

    /**
     * 构造一个预算记账本。
     *
     * @param namespace    命名空间
     * @param epsilonTotal epsilon 总预算，必须非负
     * @param deltaTotal   delta 总预算，必须非负
     * @throws IllegalArgumentException 当 namespace 为空或预算为负时抛出
     */
    public BudgetAccountant(String namespace, double epsilonTotal, double deltaTotal) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be empty");
        }
        if (epsilonTotal < 0 || deltaTotal < 0) {
            throw new IllegalArgumentException(
                String.format("budget must be non-negative: epsilon=%.4f, delta=%.6f", epsilonTotal, deltaTotal)
            );
        }
        this.namespace = namespace;
        this.epsilonTotal = epsilonTotal;
        this.deltaTotal = deltaTotal;
    }

    /**
     * 获取或创建指定 namespace 的预算记账本单例。
     * <p>
     * 若该 namespace 已存在实例，则返回已有实例；
     * 若请求的总预算与已有实例不一致，则抛出 IllegalArgumentException，避免静默使用错误预算。
     * 若不存在，则使用传入参数创建新实例。
     * </p>
     *
     * @param namespace    命名空间
     * @param epsilonTotal epsilon 总预算
     * @param deltaTotal   delta 总预算
     * @return 该 namespace 对应的 {@link BudgetAccountant} 实例
     * @throws IllegalArgumentException 当 namespace 为空或预算不一致时抛出
     */
    public static BudgetAccountant getInstance(String namespace, double epsilonTotal, double deltaTotal) {
        if (namespace == null || namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace must not be empty");
        }
        BudgetAccountant existing = INSTANCES.get(namespace);
        if (existing != null) {
            if (Double.compare(existing.epsilonTotal, epsilonTotal) != 0
                || Double.compare(existing.deltaTotal, deltaTotal) != 0) {
                throw new IllegalArgumentException(
                    String.format(
                        "Budget mismatch for namespace '%s': existing=(epsilon=%.4f, delta=%.6f), requested=(epsilon=%.4f, delta=%.6f)",
                        namespace, existing.epsilonTotal, existing.deltaTotal, epsilonTotal, deltaTotal
                    )
                );
            }
            return existing;
        }
        synchronized (BudgetAccountant.class) {
            existing = INSTANCES.get(namespace);
            if (existing != null) {
                if (Double.compare(existing.epsilonTotal, epsilonTotal) != 0
                    || Double.compare(existing.deltaTotal, deltaTotal) != 0) {
                    throw new IllegalArgumentException(
                        String.format(
                            "Budget mismatch for namespace '%s': existing=(epsilon=%.4f, delta=%.6f), requested=(epsilon=%.4f, delta=%.6f)",
                            namespace, existing.epsilonTotal, existing.deltaTotal, epsilonTotal, deltaTotal
                        )
                    );
                }
                return existing;
            }
            BudgetAccountant created = new BudgetAccountant(namespace, epsilonTotal, deltaTotal);
            INSTANCES.put(namespace, created);
            return created;
        }
    }

    /**
     * 消耗指定数量的 epsilon 与 delta 预算。
     *
     * @param epsilon 本次消耗的 epsilon
     * @param delta   本次消耗的 delta
     * @throws PrivacyBudgetExhaustedException 当总消耗超过总预算时抛出
     */
    public synchronized void spend(double epsilon, double delta) {
        double newEpsilon = this.epsilonSpent + epsilon;
        double newDelta = this.deltaSpent + delta;
        if (newEpsilon > epsilonTotal || newDelta > deltaTotal) {
            throw new PrivacyBudgetExhaustedException(
                String.format("Privacy budget exhausted in namespace %s: epsilon=%.4f/%.4f, delta=%.6f/%.6f",
                    namespace, newEpsilon, epsilonTotal, newDelta, deltaTotal)
            );
        }
        this.epsilonSpent = newEpsilon;
        this.deltaSpent = newDelta;
    }

    /**
     * 查询剩余预算。
     *
     * @return 包含 "epsilon" 与 "delta" 两个键的剩余预算映射
     */
    public synchronized Map<String, Double> remaining() {
        return Map.of(
            "epsilon", epsilonTotal - epsilonSpent,
            "delta", deltaTotal - deltaSpent
        );
    }
}
