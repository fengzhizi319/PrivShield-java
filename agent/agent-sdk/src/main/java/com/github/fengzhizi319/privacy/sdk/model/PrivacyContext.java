package com.github.fengzhizi319.privacy.sdk.model;

import java.util.Collections;
import java.util.Map;

/**
 * 隐私上下文模型，用于描述一次隐私计算请求的上下文信息。
 * <p>
 * 包含命名空间、项目 ID、用户角色、使用目的以及字段敏感度标签等信息，
 * 供 {@link com.github.fengzhizi319.privacy.sdk.util.ParameterResolver} 在解析参数时作为上下文覆盖依据。
 * </p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class PrivacyContext {

    /** 命名空间，默认值为 "default"，用于隔离不同租户或环境的隐私预算与配置。 */
    private String namespace = "default";

    /** 项目 ID，标识当前请求所属的项目。 */
    private String projectId;

    /** 用户角色，例如 admin、researcher 等，可用于未来做基于角色的策略控制。 */
    private String userRole;

    /** 数据使用目的，例如 "model_training"、"statistics" 等。 */
    private String purpose;

    /** 字段敏感度标签映射，key 为字段名，value 为敏感度等级或标签对象。 */
    private Map<String, Object> sensitivityTags = Collections.emptyMap();

    /**
     * 默认构造器，创建一个所有字段均为默认值的上下文。
     */
    public PrivacyContext() {
    }

    /**
     * 静态工厂方法，根据使用目的快速创建上下文。
     *
     * @param purpose 数据使用目的
     * @return 已设置 purpose 的 {@link PrivacyContext} 实例
     */
    public static PrivacyContext of(String purpose) {
        PrivacyContext ctx = new PrivacyContext();
        ctx.setPurpose(purpose);
        return ctx;
    }

    /**
     * 获取命名空间。
     *
     * @return 命名空间字符串
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * 设置命名空间。
     *
     * @param namespace 命名空间字符串
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * 获取项目 ID。
     *
     * @return 项目 ID 字符串
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * 设置项目 ID。
     *
     * @param projectId 项目 ID 字符串
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * 获取用户角色。
     *
     * @return 用户角色字符串
     */
    public String getUserRole() {
        return userRole;
    }

    /**
     * 设置用户角色。
     *
     * @param userRole 用户角色字符串
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    /**
     * 获取使用目的。
     *
     * @return 使用目的字符串
     */
    public String getPurpose() {
        return purpose;
    }

    /**
     * 设置使用目的。
     *
     * @param purpose 使用目的字符串
     */
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    /**
     * 获取字段敏感度标签映射。
     *
     * @return 敏感度标签映射，不会为 {@code null}
     */
    public Map<String, Object> getSensitivityTags() {
        return sensitivityTags;
    }

    /**
     * 设置字段敏感度标签映射。
     *
     * @param sensitivityTags 敏感度标签映射；若为 {@code null} 则重置为空映射
     */
    public void setSensitivityTags(Map<String, Object> sensitivityTags) {
        this.sensitivityTags = sensitivityTags == null ? Collections.emptyMap() : sensitivityTags;
    }
}
