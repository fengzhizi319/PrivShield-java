package com.github.fengzhizi319.privacy.sdk.exception;

/**
 * 隐私计算 SDK 的基础运行时异常。
 * <p>
 * 所有 SDK 自定义异常的根类，便于调用方统一捕获与处理。
 * </p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class PrivacyException extends RuntimeException {

    /**
     * 使用指定错误消息构造异常。
     *
     * @param message 错误消息
     */
    public PrivacyException(String message) {
        super(message);
    }

    /**
     * 使用指定错误消息与底层原因构造异常。
     *
     * @param message 错误消息
     * @param cause   底层异常原因
     */
    public PrivacyException(String message, Throwable cause) {
        super(message, cause);
    }
}
