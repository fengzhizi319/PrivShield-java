package com.github.fengzhizi319.privacy.sdk.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * K-匿名 API（K-Anonymity API）。
 * <p>
 * 对单条记录中的准标识符（Quasi-Identifiers）按预定义的泛化层次结构（Generalization Hierarchy）进行泛化，
 * 使得发布后的数据集中每条记录至少与 {@code k-1} 条其他记录不可区分。
 * </p>
 *
 * <p><b>实现注意点：</b>当前版本为单记录演示实现，{@link #chooseLevel(int, GeneralizationHierarchy)}
 * 采用简化策略：k 每增加 5 提升一层泛化，实际生产应基于等价类大小动态调整。</p>
 *
 * @author fengzhizi319
 * @since 0.1.0
 */
public class KAnonymityApi {

    /**
     * 对单条记录进行 K-匿名泛化。
     * <p>
     * 仅处理 {@code qiCols} 中指定的准标识符列；若某列存在对应的泛化层次结构，则按计算出的泛化层级替换原值，
     * 非准标识符列原样保留。
     * </p>
     *
     * @param record       原始记录，key 为列名，value 为原始值
     * @param qiCols       准标识符列名列表
     * @param hierarchies  各准标识符列对应的泛化层次结构映射
     * @param k            K-匿名参数，k 越大通常泛化程度越高
     * @return 泛化后的新记录；内部为 {@link HashMap} 副本，不修改原记录
     */
    public Map<String, Object> anonymizeRecord(Map<String, Object> record,
                                               List<String> qiCols,
                                               Map<String, GeneralizationHierarchy> hierarchies,
                                               int k) {
        Map<String, Object> result = new HashMap<>(record);
        for (String col : qiCols) {
            GeneralizationHierarchy h = hierarchies != null ? hierarchies.get(col) : null;
            Object value = record.get(col);
            if (h != null && value != null) {
                result.put(col, h.generalize(value.toString(), chooseLevel(k, h)));
            }
        }
        return result;
    }

    /**
     * 根据 k 值与泛化层次最大层级选择泛化层级。
     * <p>
     * 简化规则：{@code level = max(1, min(k / 5, maxLevel))}。
     * </p>
     *
     * @param k 目标 K-匿名参数
     * @param h 泛化层次结构
     * @return 选中的泛化层级，范围 [1, maxLevel]
     */
    private int chooseLevel(int k, GeneralizationHierarchy h) {
        // Simplified: higher k => higher generalization level
        int max = h.maxLevel();
        int level = Math.min((k / 5), max);
        return Math.max(level, 1);
    }

    /**
     * 泛化层次结构接口，定义如何按层级对原始值进行泛化以及最大泛化层级。
     */
    public interface GeneralizationHierarchy {

        /**
         * 按指定层级对值进行泛化。
         *
         * @param value 原始值
         * @param level 泛化层级，0 表示不泛化
         * @return 泛化后的字符串
         */
        String generalize(String value, int level);

        /**
         * 返回该层次结构支持的最大泛化层级。
         *
         * @return 最大层级（包含）
         */
        int maxLevel();
    }

    /**
     * 年龄泛化层次结构工厂方法。
     * <p>
     * 层级说明：
     * <ul>
     *   <li>0：原值</li>
     *   <li>1：5 岁区间，如 [25-30]</li>
     *   <li>2：10 岁区间，如 [20-30]</li>
     *   <li>3：20 岁区间，如 [20-40]</li>
     *   <li>其他：完全泛化为 "*"</li>
     * </ul>
     * </p>
     *
     * @return 年龄泛化层次结构实例
     */
    public static GeneralizationHierarchy ageHierarchy() {
        return new GeneralizationHierarchy() {
            @Override
            public String generalize(String value, int level) {
                if (value == null) {
                    return "*";
                }
                try {
                    int age = Integer.parseInt(value);
                    return switch (level) {
                        case 0 -> value;
                        case 1 -> {
                            int start = (age / 5) * 5;
                            yield String.format("[%d-%d]", start, start + 5);
                        }
                        case 2 -> {
                            int start = (age / 10) * 10;
                            yield String.format("[%d-%d]", start, start + 10);
                        }
                        case 3 -> {
                            int start = (age / 20) * 20;
                            yield String.format("[%d-%d]", start, start + 20);
                        }
                        default -> "*";
                    };
                } catch (NumberFormatException e) {
                    return "*";
                }
            }

            @Override
            public int maxLevel() {
                return 4;
            }
        };
    }

    /**
     * 邮编泛化层次结构工厂方法。
     * <p>
     * 层级说明：
     * <ul>
     *   <li>0：原值</li>
     *   <li>1：保留前 3 位</li>
     *   <li>2：保留前 2 位</li>
     *   <li>3：保留前 1 位</li>
     *   <li>其他：完全泛化为 "*"</li>
     * </ul>
     * </p>
     *
     * @return 邮编泛化层次结构实例
     */
    public static GeneralizationHierarchy zipcodeHierarchy() {
        return new GeneralizationHierarchy() {
            @Override
            public String generalize(String value, int level) {
                return switch (level) {
                    case 0 -> value;
                    case 1 -> value.length() >= 3 ? value.substring(0, 3) + "***" : value;
                    case 2 -> value.length() >= 2 ? value.substring(0, 2) + "****" : value;
                    case 3 -> value.length() >= 1 ? value.charAt(0) + "*****" : value;
                    default -> "*";
                };
            }

            @Override
            public int maxLevel() {
                return 4;
            }
        };
    }

    /**
     * 性别泛化层次结构工厂方法。
     * <p>
     * 仅支持两级：0 为原值，大于等于 1 泛化为 "*"。
     * </p>
     *
     * @return 性别泛化层次结构实例
     */
    public static GeneralizationHierarchy genderHierarchy() {
        return new GeneralizationHierarchy() {
            @Override
            public String generalize(String value, int level) {
                return level >= 1 ? "*" : value;
            }

            @Override
            public int maxLevel() {
                return 1;
            }
        };
    }

    /**
     * 对整张表使用 Mondrian 多维分区算法执行 K-匿名表格泛化。
     */
    public List<Map<String, Object>> kAnonymizeTable(List<Map<String, Object>> rows, List<String> qiCols, int k, int maxDepth) {
        if (rows == null || rows.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        int n = rows.size();
        if (n < k) {
            throw new IllegalArgumentException(String.format("Input table has %d rows, but k-anonymity requires at least %d", n, k));
        }
        if (qiCols == null || qiCols.isEmpty()) {
            throw new IllegalArgumentException("qiCols must not be empty");
        }
        for (String col : qiCols) {
            if (!rows.get(0).containsKey(col)) {
                throw new IllegalArgumentException(String.format("qiCol '%s' not found in rows", col));
            }
        }

        return mondrian(rows, qiCols, k, maxDepth);
    }

    private List<Map<String, Object>> mondrian(List<Map<String, Object>> records, List<String> qiCols, int k, int depth) {
        if (records.size() < 2 * k || depth <= 0) {
            return generalize(records, qiCols);
        }
        String dim = chooseDimension(records, qiCols);
        int splitIdx = medianSplit(records, dim, k);
        if (splitIdx == -1) {
            return generalize(records, qiCols);
        }

        List<Map<String, Object>> sortedRecords = new java.util.ArrayList<>(records);
        sortRecords(sortedRecords, dim);

        List<Map<String, Object>> left = mondrian(sortedRecords.subList(0, splitIdx), qiCols, k, depth - 1);
        List<Map<String, Object>> right = mondrian(sortedRecords.subList(splitIdx, sortedRecords.size()), qiCols, k, depth - 1);

        List<Map<String, Object>> result = new java.util.ArrayList<>(left.size() + right.size());
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    private boolean isNumeric(Object val) {
        return val instanceof Number;
    }

    private double toDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0.0;
    }

    private double span(List<Map<String, Object>> records, String col) {
        java.util.List<Object> vals = new java.util.ArrayList<>();
        for (Map<String, Object> r : records) {
            Object v = r.get(col);
            if (v != null) {
                vals.add(v);
            }
        }
        if (vals.isEmpty()) {
            return 0.0;
        }
        boolean allNum = true;
        for (Object v : vals) {
            if (!isNumeric(v)) {
                allNum = false;
                break;
            }
        }
        if (allNum) {
            double minVal = toDouble(vals.get(0));
            double maxVal = toDouble(vals.get(0));
            for (Object v : vals) {
                double f = toDouble(v);
                if (f < minVal) minVal = f;
                if (f > maxVal) maxVal = f;
            }
            return maxVal - minVal;
        }

        java.util.Set<String> unique = new java.util.HashSet<>();
        for (Object v : vals) {
            unique.add(v.toString());
        }
        return unique.size() - 1;
    }

    private String chooseDimension(List<Map<String, Object>> records, List<String> qiCols) {
        String maxCol = qiCols.get(0);
        double maxSpan = span(records, maxCol);
        for (int i = 1; i < qiCols.size(); i++) {
            String col = qiCols.get(i);
            double s = span(records, col);
            if (s > maxSpan) {
                maxSpan = s;
                maxCol = col;
            }
        }
        return maxCol;
    }

    private int medianSplit(List<Map<String, Object>> records, String dim, int k) {
        int n = records.size();
        if (n < 2 * k) {
            return -1;
        }

        List<Map<String, Object>> sortedRecords = new java.util.ArrayList<>(records);
        sortRecords(sortedRecords, dim);

        int mid = n / 2;
        int splitIdx = Math.max(k, Math.min(mid, n - k));
        if (splitIdx < k || n - splitIdx < k) {
            return -1;
        }
        return splitIdx;
    }

    private void sortRecords(List<Map<String, Object>> records, String dim) {
        records.sort((r1, r2) -> {
            Object v1 = r1.get(dim);
            Object v2 = r2.get(dim);
            if (isNumeric(v1) && isNumeric(v2)) {
                return Double.compare(toDouble(v1), toDouble(v2));
            }
            String s1 = v1 == null ? "" : v1.toString();
            String s2 = v2 == null ? "" : v2.toString();
            return s1.compareTo(s2);
        });
    }

    private List<Map<String, Object>> generalize(List<Map<String, Object>> records, List<String> qiCols) {
        if (records == null || records.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        Map<String, Object> generalized = new HashMap<>();
        for (String col : qiCols) {
            java.util.List<Object> vals = new java.util.ArrayList<>();
            for (Map<String, Object> r : records) {
                Object v = r.get(col);
                if (v != null) {
                    vals.add(v);
                }
            }
            if (vals.isEmpty()) {
                continue;
            }
            boolean allNum = true;
            for (Object v : vals) {
                if (!isNumeric(v)) {
                    allNum = false;
                    break;
                }
            }
            if (allNum) {
                double minVal = toDouble(vals.get(0));
                double maxVal = toDouble(vals.get(0));
                for (Object v : vals) {
                    double f = toDouble(v);
                    if (f < minVal) minVal = f;
                    if (f > maxVal) maxVal = f;
                }
                if (minVal == maxVal) {
                    generalized.put(col, minVal);
                } else {
                    // Check if they are integers to avoid floating point formatting if possible
                    if (minVal == (long) minVal && maxVal == (long) maxVal) {
                        generalized.put(col, String.format("[%d-%d]", (long) minVal, (long) maxVal));
                    } else {
                        generalized.put(col, String.format("[%s-%s]", String.valueOf(minVal), String.valueOf(maxVal)));
                    }
                }
            } else {
                java.util.Set<String> uniqueSet = new java.util.HashSet<>();
                for (Object v : vals) {
                    uniqueSet.add(v.toString());
                }
                java.util.List<String> unique = new java.util.ArrayList<>(uniqueSet);
                java.util.Collections.sort(unique);
                if (unique.size() == 1) {
                    generalized.put(col, unique.get(0));
                } else {
                    StringBuilder sb = new StringBuilder("{");
                    for (int i = 0; i < unique.size(); i++) {
                        if (i > 0) {
                            sb.append(",");
                        }
                        sb.append(unique.get(i));
                    }
                    sb.append("}");
                    generalized.put(col, sb.toString());
                }
            }
        }

        List<Map<String, Object>> result = new java.util.ArrayList<>(records.size());
        for (Map<String, Object> r : records) {
            Map<String, Object> newRec = new HashMap<>(r);
            newRec.putAll(generalized);
            result.add(newRec);
        }
        return result;
    }
}
