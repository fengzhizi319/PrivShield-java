package com.github.fengzhizi319.privacy.sdk.exception;

/**
 * 隐私预算耗尽异常。
 * <p>
 * 当某 namespace 下的 epsilon 或 delta 总消耗超过总预算时，
 * {@link com.github.fengzhizi319.privacy.sdk.util.BudgetAccountant#spend(double, double)} 会抛出此异常。
 * </p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class PrivacyBudgetExhaustedException extends PrivacyException {

    /**
     * 使用指定错误消息构造异常。
     *
     * @param message 错误消息，通常包含 namespace 与已消耗/总预算信息
     */
    public PrivacyBudgetExhaustedException(String message) {
        super(message);
    }
}
