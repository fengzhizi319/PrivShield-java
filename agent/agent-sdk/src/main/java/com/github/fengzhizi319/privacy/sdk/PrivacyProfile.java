package com.github.fengzhizi319.privacy.sdk;

import com.github.fengzhizi319.privacy.sdk.util.ParameterResolver;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 隐私配置 profile，用于从 YAML 文件或空配置构建统一的参数解析器。
 * <p>
 * 本身为不可变值对象，内部持有 {@link ParameterResolver}，供各隐私原语在运行时解析参数。
 * </p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public final class PrivacyProfile {

    /** 内部参数解析器，负责合并默认值、profile 配置、上下文与请求参数。 */
    private final ParameterResolver resolver;

    /**
     * 私有构造器，强制通过静态工厂方法创建实例。
     *
     * @param resolver 参数解析器
     */
    private PrivacyProfile(ParameterResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * 构造一个空配置 profile，解析器内部不含任何外部配置。
     *
     * @return 新的 {@link PrivacyProfile} 实例
     */
    public static PrivacyProfile empty() {
        return new PrivacyProfile(new ParameterResolver());
    }

    /**
     * 从 YAML 文件路径（字符串形式）构造 profile。
     *
     * @param path YAML 文件路径字符串；若文件不存在，则等效于空配置
     * @return 新的 {@link PrivacyProfile} 实例
     * @throws RuntimeException 当 YAML 解析失败时抛出
     */
    public static PrivacyProfile fromYaml(String path) {
        return new PrivacyProfile(new ParameterResolver(Paths.get(path)));
    }

    /**
     * 从 YAML 文件路径（{@link Path} 形式）构造 profile。
     *
     * @param path YAML 文件路径；若为 {@code null} 或文件不存在，则等效于空配置
     * @return 新的 {@link PrivacyProfile} 实例
     * @throws RuntimeException 当 YAML 解析失败时抛出
     */
    public static PrivacyProfile fromYaml(Path path) {
        return new PrivacyProfile(new ParameterResolver(path));
    }

    /**
     * 获取内部参数解析器。
     *
     * @return {@link ParameterResolver} 实例
     */
    public ParameterResolver getResolver() {
        return resolver;
    }

    /**
     * 获取配置中定义的命名空间。
     *
     * @return 命名空间字符串；若未定义或解析器为空，则返回 "default"
     */
    public String getNamespace() {
        if (resolver != null && resolver.getProfile() != null) {
            Object ns = resolver.getProfile().get("namespace");
            if (ns != null) {
                return ns.toString();
            }
        }
        return "default";
    }
}
