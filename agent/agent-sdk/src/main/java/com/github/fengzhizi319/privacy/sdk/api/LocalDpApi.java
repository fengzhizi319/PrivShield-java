package com.github.fengzhizi319.privacy.sdk.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 本地差分隐私 API（Local Differential Privacy API）。
 * <p>
 * 提供二值随机响应（Binary Randomized Response）与多类别随机响应（Categorical Randomized Response），
 * 以及对应的无偏频率估计器。所有计算纯本地完成，不依赖外部网络。
 * </p>
 * <p>
 * 与中心化 DP 不同，本地 DP 在数据持有者侧即完成扰动，无需可信聚合器。
 * </p>
 * <p>本类线程安全：默认使用 {@link ThreadLocalRandom}，测试时可注入固定种子的 {@link Random}。</p>
 *
 * @author fengzhizi319
 * @since 0.2.0
 */
public class LocalDpApi {

    /** 随机数生成器。为 null 时使用 ThreadLocalRandom（线程安全且无竞争）。 */
    private final Random random;

    /**
     * 使用默认随机数生成器构造 API（ThreadLocalRandom，线程安全）。
     */
    public LocalDpApi() {
        this.random = null;
    }

    /**
     * 使用指定随机数生成器构造 API，便于测试时注入固定种子。
     *
     * @param random 随机数生成器
     */
    public LocalDpApi(Random random) {
        this.random = random;
    }

    /** 获取当前线程安全的随机数生成器。 */
    private Random rng() {
        return random != null ? random : ThreadLocalRandom.current();
    }

    /**
     * 对二值数据（0/1）批量执行本地 DP 扰动（随机响应）。
     * <p>
     * 使用 Warner 随机响应模型：以概率 p = e^ε / (1 + e^ε) 保留真实值，否则翻转。
     * </p>
     *
     * @param values  二值数据列表（0 或 1）
     * @param epsilon 隐私预算
     * @return 扰动后的二值数据列表
     */
    public List<Integer> perturbBinaryBatch(List<Integer> values, double epsilon) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        // 计算保留概率 p = e^ε / (1 + e^ε)
        double p = Math.exp(epsilon) / (1.0 + Math.exp(epsilon));

        List<Integer> result = new ArrayList<>(values.size());
        for (int v : values) {
            if (rng().nextDouble() < p) {
                // 保留真实值 / Keep true value
                result.add(v);
            } else {
                // 翻转 / Flip
                result.add(1 - v);
            }
        }
        return result;
    }

    /**
     * 对类别型数据批量执行本地 DP 扰动（多类别随机响应）。
     * <p>
     * 以概率 p = e^ε / (e^ε + k - 1) 保留真实类别，否则从其余 k-1 个类别中均匀随机选取。
     * 其中 k 为类别总数。
     * </p>
     *
     * @param values     类别型数据列表
     * @param categories 所有可能的类别列表
     * @param epsilon    隐私预算
     * @return 扰动后的类别数据列表
     */
    public List<String> perturbCategoricalBatch(List<String> values, List<String> categories, double epsilon) {
        if (values == null || values.isEmpty()) {
            return new ArrayList<>();
        }
        int k = categories.size();
        if (k <= 1) {
            return new ArrayList<>(values);
        }

        // 保留概率 / Retention probability
        double p = Math.exp(epsilon) / (Math.exp(epsilon) + k - 1.0);

        // 构建类别索引 / Build category index
        Map<String, Integer> catIndex = new HashMap<>();
        for (int i = 0; i < categories.size(); i++) {
            catIndex.put(categories.get(i), i);
        }

        List<String> result = new ArrayList<>(values.size());
        for (String v : values) {
            if (rng().nextDouble() < p) {
                // 保留真实类别 / Keep true category
                result.add(v);
            } else {
                // 从其余类别中均匀随机选取 / Uniformly select from other categories
                Integer origIdx = catIndex.get(v);
                if (origIdx == null) {
                    // 未知类别，随机选一个 / Unknown category, pick random
                    result.add(categories.get(rng().nextInt(k)));
                } else {
                    // 生成 [0, k-2] 范围内的随机索引，映射到排除 origIdx 后的类别
                    int r = rng().nextInt(k - 1);
                    if (r >= origIdx) {
                        r++;
                    }
                    result.add(categories.get(r));
                }
            }
        }
        return result;
    }

    /**
     * 根据扰动后的二值样本估计真实比例为 1 的频率。
     * <p>
     * 使用无偏估计器：f_hat = (reported_fraction - (1-p)) / (2p - 1)，
     * 其中 p = e^ε / (1 + e^ε)。结果裁剪到 [0, 1] 区间。
     * </p>
     *
     * @param reported 扰动后的二值样本
     * @param epsilon  隐私预算
     * @return 估计的真实频率
     */
    public double estimateBinaryFrequency(List<Integer> reported, double epsilon) {
        if (reported == null || reported.isEmpty()) {
            return 0.0;
        }
        double p = Math.exp(epsilon) / (1.0 + Math.exp(epsilon));

        // 计算报告为 1 的比例 / Compute reported fraction of 1s
        long count1 = reported.stream().filter(v -> v == 1).count();
        double reportedFraction = (double) count1 / reported.size();

        // 无偏估计 / Unbiased estimation
        double denominator = 2.0 * p - 1.0;
        if (Math.abs(denominator) < 1e-10) {
            return 0.5;
        }
        double estimate = (reportedFraction - (1.0 - p)) / denominator;

        // 裁剪到 [0, 1] / Clamp to [0, 1]
        return Math.max(0.0, Math.min(1.0, estimate));
    }

    /**
     * 根据扰动后的类别样本估计各类别的真实频率。
     * <p>
     * 使用无偏估计器：对每个类别 c，
     * f_hat(c) = (reported_fraction(c) - q) / (p - q)，
     * 其中 p = e^ε / (e^ε + k - 1)，q = (1-p) / (k-1)。
     * 结果裁剪到 [0, 1] 区间并归一化使总和为 1。
     * </p>
     *
     * @param reported   扰动后的类别样本
     * @param categories 所有可能的类别列表
     * @param epsilon    隐私预算
     * @return 各类别估计频率的映射
     */
    public Map<String, Double> estimateCategoricalHistogram(List<String> reported, List<String> categories, double epsilon) {
        Map<String, Double> result = new HashMap<>();
        if (reported == null || reported.isEmpty()) {
            for (String c : categories) {
                result.put(c, 0.0);
            }
            return result;
        }

        int k = categories.size();
        if (k <= 1) {
            result.put(categories.get(0), 1.0);
            return result;
        }

        double p = Math.exp(epsilon) / (Math.exp(epsilon) + k - 1.0);
        double q = (1.0 - p) / (k - 1.0);

        // 统计报告频率 / Count reported frequencies
        Map<String, Integer> reportedCounts = new HashMap<>();
        for (String v : reported) {
            reportedCounts.merge(v, 1, Integer::sum);
        }
        double n = reported.size();

        // 无偏估计 / Unbiased estimation
        double denominator = p - q;
        if (Math.abs(denominator) < 1e-10) {
            for (String c : categories) {
                result.put(c, 1.0 / k);
            }
            return result;
        }

        double total = 0.0;
        for (String c : categories) {
            double reportedFraction = reportedCounts.getOrDefault(c, 0) / n;
            double estimate = (reportedFraction - q) / denominator;
            estimate = Math.max(0.0, Math.min(1.0, estimate));
            result.put(c, estimate);
            total += estimate;
        }

        // 归一化 / Normalize
        if (total > 0) {
            for (String c : categories) {
                result.put(c, result.get(c) / total);
            }
        }
        return result;
    }
}
