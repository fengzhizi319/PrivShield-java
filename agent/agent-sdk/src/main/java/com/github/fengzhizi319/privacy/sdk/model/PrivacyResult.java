package com.github.fengzhizi319.privacy.sdk.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 隐私计算结果封装类（Privacy Result Wrapper）。
 * <p>
 * 用于统一返回隐私原语执行后的数据、实际使用的参数、可审计证明（proof）以及警告信息。
 * </p>
 *
 * @param <T> 结果数据的实际类型
 * @author fengzhizi319
 * @since 0.1.0
 */
public class PrivacyResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 隐私计算返回的核心数据。 */
    private final T data;

    /** 实际使用的参数映射，便于审计与复现。 */
    private final Map<String, Object> paramsUsed;

    /** 可审计证明信息，例如预算消耗、泛化层级等。 */
    private final Map<String, Object> proof;

    /** 警告信息列表，例如预算接近耗尽、参数fallback等。 */
    private final List<String> warnings;

    /**
     * 构造一个完整的结果对象。
     *
     * @param data       核心结果数据
     * @param paramsUsed 实际使用的参数映射
     * @param proof      可审计证明信息映射
     * @param warnings   警告信息列表；若为 {@code null} 则重置为空列表
     */
    public PrivacyResult(T data, Map<String, Object> paramsUsed, Map<String, Object> proof, List<String> warnings) {
        this.data = data;
        this.paramsUsed = paramsUsed;
        this.proof = proof;
        this.warnings = warnings == null ? Collections.emptyList() : warnings;
    }

    /**
     * 获取核心结果数据。
     *
     * @return 结果数据对象
     */
    public T getData() {
        return data;
    }

    /**
     * 获取实际使用的参数映射（不可变视图）。
     *
     * @return 参数映射
     */
    public Map<String, Object> getParamsUsed() {
        return paramsUsed == null ? Collections.emptyMap() : Collections.unmodifiableMap(paramsUsed);
    }

    /**
     * 获取可审计证明信息（不可变视图）。
     *
     * @return 证明信息映射
     */
    public Map<String, Object> getProof() {
        return proof == null ? Collections.emptyMap() : Collections.unmodifiableMap(proof);
    }

    /**
     * 获取警告信息列表（不可变视图）。
     *
     * @return 警告信息列表，不会为 {@code null}
     */
    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
}
