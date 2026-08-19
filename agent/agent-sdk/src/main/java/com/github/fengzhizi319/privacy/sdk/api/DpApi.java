package com.github.fengzhizi319.privacy.sdk.api;

import com.github.fengzhizi319.privacy.sdk.util.BudgetAccountant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 差分隐私 API（Differential Privacy API）。
 * <p>
 * 提供计数（count）、求和（sum）、均值（mean）等聚合统计量的加噪能力，支持拉普拉斯（Laplace）与高斯（Gaussian）机制。
 * 并通过 {@link BudgetAccountant} 记录隐私预算消耗。
 * </p>
 * <p>本类线程安全：默认使用 {@link ThreadLocalRandom}，测试时可注入固定种子的 {@link Random}。</p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class DpApi {

    /** 隐私预算记账本，用于追踪 epsilon/delta 消耗。 */
    private final BudgetAccountant budget;

    /** 随机数生成器，用于采样随机噪声。为 null 时使用 ThreadLocalRandom（线程安全且无竞争）。 */
    private final Random random;

    /**
     * 使用默认预算构造 API。
     * <p>
     * 默认预算 namespace 为 "default"，epsilon=10.0，delta=1e-4。
     * 随机数使用 {@link ThreadLocalRandom}，线程安全且高并发无锁竞争。
     * </p>
     */
    public DpApi() {
        this(BudgetAccountant.getInstance("default", 10.0, 1e-4));
    }

    /**
     * 使用指定预算构造 API，随机数使用 ThreadLocalRandom。
     *
     * @param budget 隐私预算记账本
     */
    public DpApi(BudgetAccountant budget) {
        this.budget = budget;
        this.random = null; // use ThreadLocalRandom
    }

    /**
     * 使用指定预算与指定随机数生成器构造 API。
     *
     * @param budget 隐私预算记账本
     * @param random 随机数生成器，便于测试时注入固定种子
     */
    public DpApi(BudgetAccountant budget, Random random) {
        this.budget = budget;
        this.random = random;
    }

    /** 获取当前线程安全的随机数生成器。 */
    private Random rng() {
        return random != null ? random : ThreadLocalRandom.current();
    }

    /**
     * 统一校验 epsilon/delta/mechanism，返回规范化后的小写机制名。
     * Centralized epsilon/delta/mechanism validation; returns the normalized lower-case mechanism.
     */
    private String validateParams(double epsilon, double delta, String mechanism) {
        if (epsilon <= 0) {
            throw new IllegalArgumentException("epsilon must be positive, got " + epsilon);
        }
        String mech = mechanism == null ? "laplace" : mechanism.trim().toLowerCase();
        if (!"laplace".equals(mech) && !"gaussian".equals(mech)) {
            throw new IllegalArgumentException("unsupported mechanism: " + mechanism);
        }
        if ("gaussian".equals(mech) && delta <= 0) {
            throw new IllegalArgumentException("delta must be positive for Gaussian mechanism");
        }
        return mech;
    }

    /**
     * 对非零且非空的列表元素进行差分隐私计数。
     */
    public double count(List<Double> values, double epsilon, double delta, String mechanism) {
        long count = values.stream().filter(v -> v != null).count();
        return count(count, epsilon, delta, mechanism);
    }

    /**
     * 对真实计数值注入噪声。
     */
    public double count(long trueCount, double epsilon, double delta, String mechanism) {
        String mech = validateParams(epsilon, delta, mechanism);
        budget.spend(epsilon, delta);

        double noise;
        if ("laplace".equals(mech)) {
            noise = sampleLaplace(1.0 / epsilon);
        } else {
            double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * 1.0 / epsilon;
            noise = sampleGaussian(sigma);
        }
        return Math.max(0.0, trueCount + noise);
    }

    /**
     * 对列表元素进行差分隐私求和。
     */
    public double sum(List<Double> values, double epsilon, double delta, String mechanism, Double clipLower, Double clipUpper) {
        String mech = validateParams(epsilon, delta, mechanism);

        // Resolve and validate clip bounds before spending budget.
        double lower = 0;
        double upper = 0;
        if (clipLower != null && clipUpper != null) {
            if (clipLower > clipUpper) {
                throw new IllegalArgumentException("clipLower must be <= clipUpper");
            }
            lower = clipLower;
            upper = clipUpper;
        } else if (clipLower != null || clipUpper != null) {
            throw new IllegalArgumentException("clipLower and clipUpper must be both provided or both omitted");
        } else {
            if ("gaussian".equals(mech)) {
                throw new IllegalArgumentException("clipLower and clipUpper are required for Gaussian mechanism");
            }
            if (values != null && !values.isEmpty()) {
                lower = upper = (values.get(0) == null ? 0.0 : values.get(0));
                for (Double v : values) {
                    double val = v == null ? 0.0 : v;
                    if (val < lower) {
                        lower = val;
                    }
                    if (val > upper) {
                        upper = val;
                    }
                }
            }
        }

        budget.spend(epsilon, delta);

        double trueSum = 0.0;
        for (Double v : values) {
            double val = v == null ? 0.0 : v;
            if (val < lower) {
                val = lower;
            }
            if (val > upper) {
                val = upper;
            }
            trueSum += val;
        }

        double sensitivity = upper - lower;
        if (sensitivity <= 0) {
            sensitivity = 0.0;
        }

        double noise;
        if ("laplace".equals(mech)) {
            double scale = sensitivity / epsilon;
            noise = sampleLaplace(scale);
        } else {
            if (sensitivity > 0) {
                double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * sensitivity / epsilon;
                noise = sampleGaussian(sigma);
            } else {
                noise = 0.0;
            }
        }
        return trueSum + noise;
    }

    /**
     * 对列表元素进行差分隐私均值估计。
     */
    public double mean(List<Double> values, double epsilon, double delta, String mechanism, Double clipLower, Double clipUpper) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        validateParams(epsilon, delta, mechanism);

        double noisyCount = count(values.size(), epsilon / 2.0, delta / 2.0, mechanism);
        double noisySum = sum(values, epsilon / 2.0, delta / 2.0, mechanism, clipLower, clipUpper);
        return noisyCount > 0 ? noisySum / noisyCount : 0.0;
    }

    /**
     * 从拉普拉斯分布中采样一个随机噪声值。
     */
    private double sampleLaplace(double scale) {
        double u = rng().nextDouble() - 0.5;
        return -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
    }

    /**
     * 从高斯分布中采样一个随机噪声值。
     */
    private double sampleGaussian(double sigma) {
        return rng().nextGaussian() * sigma;
    }

    // --- 高级 DP 算子 / Advanced DP Primitives ---

    /**
     * 返回带差分隐私噪声的直方图计数（联合敏感度为 1）。
     * <p>
     * 对每个类别桶注入独立噪声，适用于分类数据的频率分布发布。
     * </p>
     *
     * @param values     输入类别值列表
     * @param categories 目标类别集合
     * @param epsilon    隐私预算 epsilon
     * @param delta      高斯机制所须 delta
     * @param mechanism  噪声机制（"laplace" 或 "gaussian"）
     * @return 分桶名到带噪计数的映射
     */
    public Map<String, Double> histogram(List<String> values, List<String> categories, double epsilon, double delta, String mechanism) {
        String mech = validateParams(epsilon, delta, mechanism);
        budget.spend(epsilon, delta);

        Map<String, Double> trueCounts = new HashMap<>(categories.size() * 2);
        for (String cat : categories) {
            trueCounts.put(cat, 0.0);
        }
        for (String v : values) {
            Double c = trueCounts.get(v);
            if (c != null) {
                trueCounts.put(v, c + 1.0);
            }
        }

        Map<String, Double> result = new HashMap<>(trueCounts.size());
        for (Map.Entry<String, Double> entry : trueCounts.entrySet()) {
            double noise;
            if ("laplace".equals(mech)) {
                noise = sampleLaplace(1.0 / epsilon);
            } else {
                double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * 1.0 / epsilon;
                noise = sampleGaussian(sigma);
            }
            result.put(entry.getKey(), Math.max(0.0, entry.getValue() + noise));
        }
        return result;
    }

    /**
     * 对已经聚合好的计数结果注入 DP 噪声。
     *
     * @param trueCount 真实计数值
     * @param epsilon   隐私预算
     * @param delta     高斯机制所须 delta
     * @param mechanism 噪声机制
     * @return 带噪声的计数值
     */
    public double noisyCount(double trueCount, double epsilon, double delta, String mechanism) {
        String mech = validateParams(epsilon, delta, mechanism);
        budget.spend(epsilon, delta);

        double noise;
        if ("laplace".equals(mech)) {
            noise = sampleLaplace(1.0 / epsilon);
        } else {
            double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * 1.0 / epsilon;
            noise = sampleGaussian(sigma);
        }
        return Math.max(0.0, trueCount + noise);
    }

    /**
     * 对已经聚合好的求和结果注入 DP 噪声。
     *
     * @param trueSum     真实求和值
     * @param sensitivity 查询敏感度（通常为 clip_upper - clip_lower）
     * @param epsilon     隐私预算
     * @param delta       高斯机制所须 delta
     * @param mechanism   噪声机制
     * @return 带噪声的求和结果
     */
    public double noisySum(double trueSum, double sensitivity, double epsilon, double delta, String mechanism) {
        String mech = validateParams(epsilon, delta, mechanism);
        budget.spend(epsilon, delta);

        double noise;
        if ("laplace".equals(mech)) {
            noise = sampleLaplace(sensitivity / epsilon);
        } else {
            double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * sensitivity / epsilon;
            noise = sampleGaussian(sigma);
        }
        return trueSum + noise;
    }

    /**
     * 对已经聚合好的 sum/count 注入 DP 噪声后得到均值。
     * 预算按 epsilon/2 分配给 count 和 sum 两部分。
     *
     * @param trueSum   真实求和值
     * @param trueCount 真实计数值
     * @param sensitivity 敏感度
     * @param epsilon   隐私预算
     * @param delta     高斯机制所须 delta
     * @param mechanism 噪声机制
     * @param minCount  最小计数阈值，低于此值返回 0
     * @return 带噪声的均值
     */
    public double noisyMean(double trueSum, double trueCount, double sensitivity, double epsilon, double delta, String mechanism, double minCount) {
        validateParams(epsilon, delta, mechanism);
        double noisyCount = noisyCount(trueCount, epsilon / 2.0, delta / 2.0, mechanism);
        double noisySumVal = noisySum(trueSum, sensitivity, epsilon / 2.0, delta / 2.0, mechanism);
        if (noisyCount < minCount) {
            return 0.0;
        }
        return noisySumVal / noisyCount;
    }

    /**
     * 对已经聚合好的直方图计数注入 DP 噪声。
     *
     * @param trueCounts 分桶名到真实计数的映射
     * @param epsilon    隐私预算
     * @param delta      高斯机制所须 delta
     * @param mechanism  噪声机制
     * @return 分桶名到带噪计数的映射
     */
    public Map<String, Double> noisyHistogram(Map<String, Double> trueCounts, double epsilon, double delta, String mechanism) {
        String mech = validateParams(epsilon, delta, mechanism);
        budget.spend(epsilon, delta);

        Map<String, Double> result = new HashMap<>(trueCounts.size() * 2);
        for (Map.Entry<String, Double> entry : trueCounts.entrySet()) {
            double noise;
            if ("laplace".equals(mech)) {
                noise = sampleLaplace(1.0 / epsilon);
            } else {
                double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * 1.0 / epsilon;
                noise = sampleGaussian(sigma);
            }
            result.put(entry.getKey(), Math.max(0.0, entry.getValue() + noise));
        }
        return result;
    }

    /**
     * 对高维向量执行 L2 范数截断并注入各向同性 DP 噪声（DP-SGD 风格）。
     *
     * @param vectors   输入向量列表
     * @param maxNorm   L2 截断阈值
     * @param epsilon   隐私预算
     * @param delta     高斯机制所须 delta
     * @param mechanism 噪声机制
     * @return 加噪后的向量求和
     */
    public double[] vectorSum(List<double[]> vectors, double maxNorm, double epsilon, double delta, String mechanism) {
        if (vectors == null || vectors.isEmpty()) {
            return new double[0];
        }
        String mech = validateParams(epsilon, delta, mechanism);
        budget.spend(epsilon, delta);

        int dim = vectors.get(0).length;
        double[] sum = new double[dim];

        // L2 截断并累加 / Clip by L2 norm and accumulate
        for (double[] vec : vectors) {
            double norm = 0.0;
            for (double v : vec) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            double scale = norm > maxNorm ? maxNorm / norm : 1.0;
            for (int i = 0; i < dim; i++) {
                sum[i] += vec[i] * scale;
            }
        }

        // 注入各向同性噪声 / Inject isotropic noise
        double sensitivity = maxNorm;
        for (int i = 0; i < dim; i++) {
            double noise;
            if ("laplace".equals(mech)) {
                noise = sampleLaplace(sensitivity * Math.sqrt(2.0 * dim) / epsilon);
            } else {
                double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * sensitivity * Math.sqrt(dim) / epsilon;
                noise = sampleGaussian(sigma);
            }
            sum[i] += noise;
        }
        return sum;
    }

    /**
     * 对高维向量执行 DP 均值：L2 截断 + 加噪 + noisy_count 归一化。
     *
     * @param vectors   输入向量列表
     * @param maxNorm   L2 截断阈值
     * @param epsilon   隐私预算
     * @param delta     高斯机制所须 delta
     * @param mechanism 噪声机制
     * @param minCount  最小计数阈值
     * @return 加噪后的向量均值
     */
    public double[] vectorMean(List<double[]> vectors, double maxNorm, double epsilon, double delta, String mechanism, double minCount) {
        if (vectors == null || vectors.isEmpty()) {
            return new double[0];
        }
        double noisyCount = noisyCount(vectors.size(), epsilon / 2.0, delta / 2.0, mechanism);
        if (noisyCount < minCount) {
            return new double[vectors.get(0).length];
        }
        double[] noisySum = vectorSum(vectors, maxNorm, epsilon / 2.0, delta / 2.0, mechanism);
        for (int i = 0; i < noisySum.length; i++) {
            noisySum[i] /= noisyCount;
        }
        return noisySum;
    }

    /**
     * 使用差分隐私自适应二分搜索估计数据的截断上下界。
     *
     * @param values         输入数值列表
     * @param epsilon        隐私预算
     * @param targetQuantile 目标分位数（如 0.95）
     * @param numIterations  二分搜索迭代次数
     * @param initialClip    初始截断上界
     * @return [clipLower, clipUpper] 数组
     */
    public double[] adaptiveClip(List<Double> values, double epsilon, double targetQuantile, int numIterations, double initialClip) {
        if (values == null || values.isEmpty()) {
            return new double[]{0.0, initialClip};
        }
        if (epsilon <= 0) {
            throw new IllegalArgumentException("epsilon must be positive, got " + epsilon);
        }

        double minVal = values.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double low = 0.0;
        double high = initialClip;
        double epPerIter = epsilon / numIterations;

        for (int i = 0; i < numIterations; i++) {
            double mid = (low + high) / 2.0;
            long count = values.stream().filter(v -> v > mid).count();
            double fraction = (double) count / values.size();
            double noise = sampleLaplace(1.0 / (values.size() * epPerIter));
            fraction += noise;

            if (fraction > 1.0 - targetQuantile) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return new double[]{minVal, high};
    }

    /**
     * 实现 Tau-Thresholding 差分隐私 SQL Group-By 过滤。
     * <p>
     * 按 groupCol 分组，对 targetCol 执行 agg 聚合（sum/count/mean），
     * 仅保留通过 Tau 阈值的分组。
     * </p>
     *
     * @param rows      数据行列表
     * @param groupCol  分组列名
     * @param targetCol 目标列名
     * @param agg       聚合类型（"count"/"sum"/"mean"）
     * @param epsilon   隐私预算
     * @param delta     高斯机制所须 delta
     * @param clipLower 截断下界（可为 null）
     * @param clipUpper 截断上界（可为 null）
     * @param mechanism 噪声机制
     * @return 分组名到带噪聚合值的映射
     */
    public Map<String, Double> groupBy(List<Map<String, Object>> rows, String groupCol, String targetCol,
                                       String agg, double epsilon, double delta,
                                       Double clipLower, Double clipUpper, String mechanism) {
        if (epsilon <= 0) {
            throw new IllegalArgumentException("epsilon must be positive, got " + epsilon);
        }
        String mech = mechanism == null ? "laplace" : mechanism.trim().toLowerCase();
        budget.spend(epsilon, delta);

        // 按 groupCol 分组 / Group by groupCol
        Map<String, List<Double>> groups = new HashMap<>();
        for (Map<String, Object> row : rows) {
            String key = String.valueOf(row.get(groupCol));
            double val = 0.0;
            Object tv = row.get(targetCol);
            if (tv instanceof Number) {
                val = ((Number) tv).doubleValue();
            }
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
        }

        // Tau 阈值 / Tau threshold
        double tau = 1.0 + 1.0 / epsilon;
        double sensitivity = (clipLower != null && clipUpper != null) ? clipUpper - clipLower : 1.0;

        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : groups.entrySet()) {
            List<Double> vals = entry.getValue();
            double trueAgg;
            switch (agg.toLowerCase()) {
                case "sum":
                    trueAgg = 0.0;
                    for (double v : vals) {
                        double clipped = v;
                        if (clipLower != null && clipped < clipLower) clipped = clipLower;
                        if (clipUpper != null && clipped > clipUpper) clipped = clipUpper;
                        trueAgg += clipped;
                    }
                    break;
                case "mean":
                    if (!vals.isEmpty()) {
                        double s = 0.0;
                        for (double v : vals) {
                            double clipped = v;
                            if (clipLower != null && clipped < clipLower) clipped = clipLower;
                            if (clipUpper != null && clipped > clipUpper) clipped = clipUpper;
                            s += clipped;
                        }
                        trueAgg = s / vals.size();
                    } else {
                        trueAgg = 0.0;
                    }
                    break;
                default: // count
                    trueAgg = vals.size();
                    break;
            }

            // 注入噪声 / Inject noise
            double noise;
            if ("laplace".equals(mech)) {
                noise = sampleLaplace(sensitivity / epsilon);
            } else {
                double sigma = Math.sqrt(2.0 * Math.log(1.25 / delta)) * sensitivity / epsilon;
                noise = sampleGaussian(sigma);
            }
            double noisyVal = trueAgg + noise;

            // Tau-Thresholding
            double noisyCount = vals.size() + sampleLaplace(1.0 / epsilon);
            if (noisyCount >= tau) {
                result.put(entry.getKey(), noisyVal);
            }
        }
        return result;
    }
}
