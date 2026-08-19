package com.github.fengzhizi319.privacy.sdk.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 查询混淆 API（Query Obfuscation Layer API）。
 * <p>
 * 通过向真实查询中混入若干条虚拟查询（dummy queries）并随机打乱位置，
 * 降低外部观察者通过查询内容推断用户真实意图的能力。
 * 支持语义槽位替换（Slot-Filling）与长度相近抽样（Length-Similarity）两种混淆策略。
 * 所有计算纯本地完成，不依赖外部网络。
 * </p>
 * <p>本类线程安全：默认使用 {@link ThreadLocalRandom}，测试时可注入固定种子的 {@link Random}。</p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class QolApi {

    /** 用于选择 dummy 查询与插入位置的随机数生成器。为 null 时使用 ThreadLocalRandom。 */
    private final Random random;

    /**
     * 使用默认随机数生成器构造 API（ThreadLocalRandom，线程安全）。
     */
    public QolApi() {
        this.random = null;
    }

    /**
     * 使用指定随机数生成器构造 API，便于测试时注入固定种子。
     *
     * @param random 随机数生成器
     */
    public QolApi(Random random) {
        this.random = random;
    }

    /** 获取当前线程安全的随机数生成器。 */
    private Random rng() {
        return random != null ? random : ThreadLocalRandom.current();
    }

    /**
     * 将真实查询混入指定数量的虚拟查询中，并随机插入到结果列表的某个位置。
     * 优先使用语义槽位替换策略，不足时用长度相近抽样补齐。
     *
     * @param query       真实查询字符串
     * @param numDummies  虚拟查询数量，需大于等于 0
     * @param domain      领域标识，例如 "medical"；非 medical 时使用通用语料池
     * @param medicalPool 自定义医疗领域虚假查询池（可为 null）
     * @param genericPool 自定义通用领域虚假查询池（可为 null）
     * @return 包含真实查询与虚拟查询的列表，总长度为 {@code numDummies + 1}
     */
    public List<String> obfuscateQuery(String query, int numDummies, String domain, List<String> medicalPool, List<String> genericPool) {
        return obfuscateQueryInternal(query, numDummies, domain, medicalPool, genericPool).queries;
    }

    /**
     * 将真实查询混入指定数量的虚拟查询中，返回包含元数据的 QoLResult。
     *
     * @param query       真实查询字符串
     * @param numDummies  虚拟查询数量
     * @param domain      领域标识
     * @param medicalPool 自定义医疗领域虚假查询池（可为 null）
     * @param genericPool 自定义通用领域虚假查询池（可为 null）
     * @return 包含混淆结果与元数据的 QoLResult
     */
    public QoLResult obfuscateQueryWithDetails(String query, int numDummies, String domain, List<String> medicalPool, List<String> genericPool) {
        return obfuscateQueryInternal(query, numDummies, domain, medicalPool, genericPool);
    }

    /**
     * 批量对查询进行混淆。
     *
     * @param queries     待混淆的查询列表
     * @param numDummies  每个查询生成的虚假查询数量
     * @param domain      领域标识
     * @param medicalPool 自定义医疗领域虚假查询池（可为 null）
     * @param genericPool 自定义通用领域虚假查询池（可为 null）
     * @return 混淆后的查询列表的列表
     */
    public List<List<String>> obfuscateQueryBatch(List<String> queries, int numDummies, String domain, List<String> medicalPool, List<String> genericPool) {
        List<List<String>> results = new ArrayList<>(queries.size());
        for (String q : queries) {
            results.add(obfuscateQueryInternal(q, numDummies, domain, medicalPool, genericPool).queries);
        }
        return results;
    }

    /**
     * 内部实现：执行查询混淆并返回 QoLResult。
     */
    private QoLResult obfuscateQueryInternal(String query, int numDummies, String domain, List<String> medicalPool, List<String> genericPool) {
        if (numDummies < 0) {
            throw new IllegalArgumentException("numDummies must be non-negative, got " + numDummies);
        }
        boolean isMedical = "medical".equalsIgnoreCase(domain);

        List<String> pool;
        if (isMedical) {
            pool = (medicalPool != null && !medicalPool.isEmpty()) ? medicalPool : MEDICAL_DUMMY_POOL;
        } else {
            pool = (genericPool != null && !genericPool.isEmpty()) ? genericPool : GENERIC_DUMMY_POOL;
        }

        List<String> dummies = new ArrayList<>(numDummies);
        String strategy = "length_similarity";

        // 尝试语义槽位替换策略 / Try semantic slot-filling strategy
        boolean useBuiltinPool = (isMedical && (medicalPool == null || medicalPool.isEmpty()))
                || (!isMedical && (genericPool == null || genericPool.isEmpty()));
        if (useBuiltinPool) {
            List<String> termsList = isMedical ? DISEASES : ENTITIES;
            String placeholder = isMedical ? "{disease}" : "{entity}";

            String matchedTerm = null;
            for (String t : termsList) {
                if (query.contains(t)) {
                    matchedTerm = t;
                    break;
                }
            }

            if (matchedTerm != null) {
                String template = query.replace(matchedTerm, placeholder);
                List<String> choices = new ArrayList<>();
                for (String t : termsList) {
                    if (!t.equals(matchedTerm)) {
                        choices.add(t);
                    }
                }
                if (choices.size() >= numDummies) {
                    List<String> selected = sampleN(choices, numDummies);
                    for (String st : selected) {
                        dummies.add(template.replace(placeholder, st));
                    }
                    strategy = "slot_filling";
                }
            }
        }

        // 长度相近抽样补齐（无放回）/ Length-similarity sampling without replacement to fill remaining
        int needed = numDummies - dummies.size();
        if (needed > 0) {
            if (!dummies.isEmpty()) {
                strategy = "hybrid";
            }
            List<String> filteredPool = new ArrayList<>();
            for (String p : pool) {
                if (!p.equals(query)) {
                    filteredPool.add(p);
                }
            }
            if (filteredPool.isEmpty()) {
                filteredPool = new ArrayList<>(pool);
            }

            int queryLen = query.length();
            List<String> closeCandidates = filterByLength(filteredPool, queryLen, 6);
            if (closeCandidates.isEmpty()) {
                closeCandidates = filterByLength(filteredPool, queryLen, 12);
            }
            if (closeCandidates.isEmpty()) {
                closeCandidates = filteredPool;
            }

            // 无放回抽样，避免重复 / Sample without replacement to avoid duplicates
            List<String> chosen = sampleN(closeCandidates, Math.min(needed, closeCandidates.size()));
            dummies.addAll(chosen);
            // 若候选不足，则允许重复补齐（兜底）
            while (dummies.size() < numDummies) {
                dummies.add(closeCandidates.get(rng().nextInt(closeCandidates.size())));
            }
        }

        // 将真实查询随机插入 / Insert real query at random position
        int pos = rng().nextInt(dummies.size() + 1);
        List<String> result = new ArrayList<>(dummies);
        result.add(pos, query);

        return new QoLResult(result, pos, domain.toLowerCase(), numDummies, strategy);
    }

    /**
     * 从列表中随机选取 n 个不重复元素，返回新的 ArrayList（避免返回 subList 视图）。
     * Randomly selects n distinct items and returns a new ArrayList (not a subList view).
     */
    private List<String> sampleN(List<String> items, int n) {
        if (n <= 0) {
            return new ArrayList<>();
        }
        if (n >= items.size()) {
            return new ArrayList<>(items);
        }
        List<String> copy = new ArrayList<>(items);
        Collections.shuffle(copy, rng());
        return new ArrayList<>(copy.subList(0, n));
    }

    /**
     * 筛选与 targetLen 长度差不超过 threshold 的候选。
     */
    private List<String> filterByLength(List<String> pool, int targetLen, int threshold) {
        List<String> result = new ArrayList<>();
        for (String p : pool) {
            if (Math.abs(p.length() - targetLen) <= threshold) {
                result.add(p);
            }
        }
        return result;
    }

    // --- 内置词库 / Built-in Pools ---

    /** 内置医疗领域虚假查询词库（共 20 条）。 */
    private static final List<String> MEDICAL_DUMMY_POOL = List.of(
            "高血压患者的日常饮食建议",
            "糖尿病患者运动注意事项",
            "冠心病的早期症状有哪些",
            "流感疫苗接种人群建议",
            "儿童常见过敏反应处理",
            "胃溃疡患者吃什么食物好",
            "哮喘发作时的紧急处理方法",
            "慢性支气管炎的预防措施",
            "抑郁症自我调理与心理疏导",
            "长期失眠的危害及改善建议",
            "脂肪肝患者运动处方",
            "痛风患者避免食用的食物清单",
            "过敏性鼻炎的日常防治手段",
            "颈椎病康复训练操指南",
            "偏头痛的诱发因素与缓解方式",
            "脑梗塞前兆表现及预防建议",
            "骨质疏松防摔倒安全提示",
            "带状疱疹的临床表现及治疗",
            "过敏性皮炎日常注意事项",
            "甲状腺结节患者饮食禁忌"
    );

    /** 内置通用领域虚假查询词库（共 20 条）。 */
    private static final List<String> GENERIC_DUMMY_POOL = List.of(
            "天气预报查询",
            "附近医院挂号流程",
            "健康档案如何查询",
            "医保报销比例说明",
            "体检报告解读指南",
            "公积金提取线上办理步骤",
            "个人所得税申报操作引导",
            "社保卡丢失如何在线补办",
            "市民卡网点营业时间查询",
            "生活垃圾分类最新标准",
            "最近的公共图书馆开放时间",
            "电动自行车上牌申领流程",
            "常用快递运费价格对比",
            "附近免费公共停车场推荐",
            "燃气费线上缴费使用指南",
            "自来水水质检测结果公告",
            "本地博物馆门票预约入口",
            "公交线路首末班车时间查询",
            "居住证积分申请材料清单",
            "数字证书在线更新流程"
    );

    /** 语义实体词库：疾病名称，用于医疗领域槽位替换。 */
    private static final List<String> DISEASES = List.of(
            "高血压", "糖尿病", "冠心病", "流感", "胃溃疡",
            "哮喘", "脑梗塞", "痛风", "失眠", "抑郁症", "脂肪肝",
            "肺炎", "甲状腺结节", "过敏性鼻炎", "颈椎病"
    );

    /** 语义实体词库：通用实体名称，用于通用领域槽位替换。 */
    private static final List<String> ENTITIES = List.of(
            "社保卡", "医保", "公积金", "健康档案", "体检报告",
            "居住证", "天气预报", "市民卡", "数字证书", "身份证"
    );

    /**
     * 查询混淆结果及结构化元数据包装。
     */
    public static class QoLResult {
        /** 包含真实查询与虚假 dummy 查询在内的混淆文本列表。 */
        public final List<String> queries;
        /** 真实查询在混淆列表中的索引位置。 */
        public final int realQueryIndex;
        /** 应用的混淆领域。 */
        public final String domain;
        /** 生成的 Dummy 虚假查询数量。 */
        public final int numDummies;
        /** 使用的混淆策略。 */
        public final String strategy;

        public QoLResult(List<String> queries, int realQueryIndex, String domain, int numDummies, String strategy) {
            this.queries = queries;
            this.realQueryIndex = realQueryIndex;
            this.domain = domain;
            this.numDummies = numDummies;
            this.strategy = strategy;
        }
    }
}
