package com.privshield.agent.grpc;

import com.github.fengzhizi319.privshield.grpc.*;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.FieldClassificationResult;
import com.github.fengzhizi319.privacy.sdk.dynclassification.model.SecurityTag;
import com.github.fengzhizi319.privacy.sdk.exception.PrivacyBudgetExhaustedException;
import com.privshield.agent.config.AgentProperties;
import com.privshield.agent.service.PrivacyService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * gRPC 服务实现 — 对应 Python grpc_server.py 的 PrivacyServicer。
 *
 * <p>实现 privacy.proto 定义的全部 33 个 RPC 方法，委托给 PrivacyService 编排层。</p>
 */
@GrpcService
public class PrivShieldGrpcService extends PrivacyServiceGrpc.PrivacyServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(PrivShieldGrpcService.class);

    private final PrivacyService service;
    private final String namespace;

    public PrivShieldGrpcService(PrivacyService service, AgentProperties props) {
        this.service = service;
        this.namespace = props.getNamespace();
    }

    // ─── 异常映射辅助 / Error mapping helper ───

    private void handleError(StreamObserver<?> responseObserver, Throwable t) {
        if (t instanceof PrivacyBudgetExhaustedException) {
            responseObserver.onError(Status.RESOURCE_EXHAUSTED
                    .withDescription("Privacy budget exhausted").asException());
        } else if (t instanceof IllegalArgumentException) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid request parameters").asException());
        } else {
            log.error("grpc_request_error", t);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Internal server error").asException());
        }
    }

    // ─── 脱敏类 / Masking ───

    @Override
    public void mask(MaskRequest request, StreamObserver<MaskResponse> observer) {
        try {
            String result = service.mask(request.getFieldName(), request.getValue(), request.getContext());
            observer.onNext(MaskResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void maskRecord(MaskRecordRequest request, StreamObserver<MaskRecordResponse> observer) {
        try {
            Map<String, String> result = service.maskRecord(new HashMap<>(request.getRecordMap()), request.getContext());
            observer.onNext(MaskRecordResponse.newBuilder().putAllResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void maskBatch(MaskBatchRequest request, StreamObserver<MaskBatchResponse> observer) {
        try {
            List<String> results = service.maskBatch(
                    new ArrayList<>(request.getFieldNamesList()),
                    new ArrayList<>(request.getValuesList()),
                    request.getContext());
            observer.onNext(MaskBatchResponse.newBuilder().addAllResults(results).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void maskDataFrame(MaskDataFrameRequest request, StreamObserver<MaskDataFrameResponse> observer) {
        try {
            List<Map<String, String>> data = request.getDataList().stream()
                    .map(re -> new HashMap<>(re.getFieldsMap()))
                    .collect(Collectors.toList());
            List<String> columns = request.getColumnsList().isEmpty() ? null : new ArrayList<>(request.getColumnsList());
            List<Map<String, String>> result = service.maskDataFrame(data, columns, request.getContext());
            List<RecordEntry> rows = result.stream()
                    .map(m -> RecordEntry.newBuilder().putAllFields(m).build())
                    .collect(Collectors.toList());
            observer.onNext(MaskDataFrameResponse.newBuilder().addAllData(rows).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── 哈希 / Hash ───

    @Override
    public void hash(HashRequest request, StreamObserver<HashResponse> observer) {
        try {
            String result = service.hash(request.getValue(), request.getSalt());
            observer.onNext(HashResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── DP 参数提取 / DP param extraction ───

    private record DpParams(double epsilon, double delta, String mechanism,
                             Double clipLower, Double clipUpper) {}

    private DpParams dpFromRequest(DPRequest req) {
        Double cl = (req.getClipLower() != 0.0 || req.getClipUpper() != 0.0) ? req.getClipLower() : null;
        Double cu = (req.getClipLower() != 0.0 || req.getClipUpper() != 0.0) ? req.getClipUpper() : null;
        return new DpParams(req.getEpsilon(), req.getDelta(), req.getMechanism(), cl, cu);
    }

    // ─── 差分隐私聚合 / DP Aggregation ───

    @Override
    public void dPCount(DPRequest request, StreamObserver<DPResponse> observer) {
        try {
            DpParams p = dpFromRequest(request);
            double result = service.dpCount(new ArrayList<>(request.getValuesList()), p.epsilon, p.delta, p.mechanism);
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPSum(DPRequest request, StreamObserver<DPResponse> observer) {
        try {
            DpParams p = dpFromRequest(request);
            double result = service.dpSum(new ArrayList<>(request.getValuesList()), p.epsilon, p.delta, p.mechanism, p.clipLower, p.clipUpper);
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPMean(DPRequest request, StreamObserver<DPResponse> observer) {
        try {
            DpParams p = dpFromRequest(request);
            double result = service.dpMean(new ArrayList<>(request.getValuesList()), p.epsilon, p.delta, p.mechanism, p.clipLower, p.clipUpper);
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPHistogram(DPHistogramRequest request, StreamObserver<DPHistogramResponse> observer) {
        try {
            Map<String, Double> result = service.dpHistogram(
                    new ArrayList<>(request.getValuesList()),
                    new ArrayList<>(request.getCategoriesList()),
                    request.getEpsilon(), request.getDelta(), request.getMechanism());
            observer.onNext(DPHistogramResponse.newBuilder().putAllResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── Noisy DP ───

    @Override
    public void dPNoisyCount(DPNoisyCountRequest request, StreamObserver<DPResponse> observer) {
        try {
            double result = service.dpNoisyCount(request.getTrueCount(), request.getEpsilon(), request.getDelta(), request.getMechanism());
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPNoisySum(DPNoisySumRequest request, StreamObserver<DPResponse> observer) {
        try {
            double sensitivity = request.getSensitivity();
            if (sensitivity == 0.0) {
                sensitivity = request.getClipUpper() - request.getClipLower();
            }
            double result = service.dpNoisySum(request.getTrueSum(), sensitivity, request.getEpsilon(), request.getDelta(), request.getMechanism());
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPNoisyMean(DPNoisyMeanRequest request, StreamObserver<DPResponse> observer) {
        try {
            double sensitivity = request.getSensitivity();
            if (sensitivity == 0.0) {
                sensitivity = request.getClipUpper() - request.getClipLower();
            }
            double minCount = request.getMinCount() != 0.0 ? request.getMinCount() : 5.0;
            double result = service.dpNoisyMean(request.getTrueSum(), request.getTrueCount(), sensitivity,
                    request.getEpsilon(), request.getDelta(), request.getMechanism(), minCount);
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPNoisyHistogram(DPNoisyHistogramRequest request, StreamObserver<DPHistogramResponse> observer) {
        try {
            Map<String, Double> trueCounts = new HashMap<>(request.getTrueCountsMap());
            Map<String, Double> result = service.dpNoisyHistogram(trueCounts, request.getEpsilon(), request.getDelta(), request.getMechanism());
            observer.onNext(DPHistogramResponse.newBuilder().putAllResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── 分块流式 DP / Chunked DP ───

    @Override
    public void dPChunkedCount(DPChunkedCountRequest request, StreamObserver<DPResponse> observer) {
        try {
            List<List<Double>> chunks = request.getChunksList().stream()
                    .map(c -> new ArrayList<>(c.getValuesList())).collect(Collectors.toList());
            double result = service.dpChunkedCount(chunks, request.getEpsilon(), request.getDelta(), request.getMechanism());
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPChunkedSum(DPChunkedSumRequest request, StreamObserver<DPResponse> observer) {
        try {
            List<List<Double>> chunks = request.getChunksList().stream()
                    .map(c -> new ArrayList<>(c.getValuesList())).collect(Collectors.toList());
            Double cl = (request.getClipLower() != 0.0 || request.getClipUpper() != 0.0) ? request.getClipLower() : null;
            Double cu = (request.getClipLower() != 0.0 || request.getClipUpper() != 0.0) ? request.getClipUpper() : null;
            double result = service.dpChunkedSum(chunks, request.getEpsilon(), request.getDelta(), request.getMechanism(), cl, cu);
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPChunkedMean(DPChunkedMeanRequest request, StreamObserver<DPResponse> observer) {
        try {
            List<List<Double>> chunks = request.getChunksList().stream()
                    .map(c -> new ArrayList<>(c.getValuesList())).collect(Collectors.toList());
            Double cl = (request.getClipLower() != 0.0 || request.getClipUpper() != 0.0) ? request.getClipLower() : null;
            Double cu = (request.getClipLower() != 0.0 || request.getClipUpper() != 0.0) ? request.getClipUpper() : null;
            double minCount = request.getMinCount() != 0.0 ? request.getMinCount() : 5.0;
            double result = service.dpChunkedMean(chunks, request.getEpsilon(), request.getDelta(), request.getMechanism(), cl, cu, minCount);
            observer.onNext(DPResponse.newBuilder().setResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPChunkedHistogram(DPChunkedHistogramRequest request, StreamObserver<DPHistogramResponse> observer) {
        try {
            List<List<String>> chunks = request.getChunksList().stream()
                    .map(c -> new ArrayList<>(c.getValuesList())).collect(Collectors.toList());
            Map<String, Double> result = service.dpChunkedHistogram(chunks, new ArrayList<>(request.getCategoriesList()),
                    request.getEpsilon(), request.getDelta(), request.getMechanism());
            observer.onNext(DPHistogramResponse.newBuilder().putAllResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── K-匿名 / K-Anonymity ───

    @Override
    public void kAnonymizeRecord(KAnonymizeRequest request, StreamObserver<KAnonymizeResponse> observer) {
        try {
            Map<String, String> result = service.kAnonymizeRecord(
                    new HashMap<>(request.getRecordMap()),
                    new ArrayList<>(request.getQiColsList()),
                    request.getK());
            observer.onNext(KAnonymizeResponse.newBuilder().putAllResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void kAnonymizeTable(KAnonymizeTableRequest request, StreamObserver<KAnonymizeTableResponse> observer) {
        try {
            List<Map<String, String>> rows = request.getRowsList().stream()
                    .map(r -> new HashMap<>(r.getFieldsMap())).collect(Collectors.toList());
            List<Map<String, String>> result = service.kAnonymizeTable(rows,
                    new ArrayList<>(request.getQiColsList()), request.getK(), request.getMaxDepth());
            List<RecordEntry> protoRows = result.stream()
                    .map(m -> RecordEntry.newBuilder().putAllFields(m).build())
                    .collect(Collectors.toList());
            observer.onNext(KAnonymizeTableResponse.newBuilder().addAllRows(protoRows).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void kAnonymizeDataFrame(KAnonymizeDataFrameRequest request, StreamObserver<KAnonymizeDataFrameResponse> observer) {
        try {
            List<Map<String, String>> data = request.getDataList().stream()
                    .map(r -> new HashMap<>(r.getFieldsMap())).collect(Collectors.toList());
            List<Map<String, String>> result = service.kAnonymizeTable(data,
                    new ArrayList<>(request.getQiColsList()), request.getK(), request.getMaxDepth());
            List<RecordEntry> protoData = result.stream()
                    .map(m -> RecordEntry.newBuilder().putAllFields(m).build())
                    .collect(Collectors.toList());
            observer.onNext(KAnonymizeDataFrameResponse.newBuilder().addAllData(protoData).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── 查询混淆 / Query Obfuscation ───

    @Override
    public void obfuscateQuery(ObfuscateQueryRequest request, StreamObserver<ObfuscateQueryResponse> observer) {
        try {
            int numDummies = request.getNumDummies() > 0 ? request.getNumDummies() : 3;
            String domain = request.getDomain().isEmpty() ? "medical" : request.getDomain();
            List<String> medPool = request.getMedicalPoolList().isEmpty() ? null : new ArrayList<>(request.getMedicalPoolList());
            List<String> genPool = request.getGenericPoolList().isEmpty() ? null : new ArrayList<>(request.getGenericPoolList());
            Integer seed = request.getSeed() != 0 ? request.getSeed() : null;
            List<String> result = service.obfuscateQuery(request.getQuery(), numDummies, domain, medPool, genPool, seed);
            observer.onNext(ObfuscateQueryResponse.newBuilder().addAllResult(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void obfuscateQueryBatch(ObfuscateQueryBatchRequest request, StreamObserver<ObfuscateQueryBatchResponse> observer) {
        try {
            String domain = request.getDomain().isEmpty() ? "medical" : request.getDomain();
            List<String> medPool = request.getMedicalPoolList().isEmpty() ? null : new ArrayList<>(request.getMedicalPoolList());
            List<String> genPool = request.getGenericPoolList().isEmpty() ? null : new ArrayList<>(request.getGenericPoolList());
            Integer seed = request.getSeed() != 0 ? request.getSeed() : null;
            List<List<String>> results = service.obfuscateQueryBatch(
                    new ArrayList<>(request.getQueriesList()), request.getNumDummies(), domain, medPool, genPool, seed);
            List<ObfuscateQueryResponse> protoResults = results.stream()
                    .map(r -> ObfuscateQueryResponse.newBuilder().addAllResult(r).build())
                    .collect(Collectors.toList());
            observer.onNext(ObfuscateQueryBatchResponse.newBuilder().addAllResults(protoResults).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── 健康检查 / Health ───

    @Override
    public void health(HealthRequest request, StreamObserver<HealthResponse> observer) {
        observer.onNext(HealthResponse.newBuilder().setStatus("ok").setNamespace(namespace).build());
        observer.onCompleted();
    }

    // ─── 参数推荐 / Recommend Params ───

    @Override
    public void recommendParams(RecommendRequest request, StreamObserver<RecommendResponse> observer) {
        try {
            List<Double> values = request.getValuesList().isEmpty() ? null : new ArrayList<>(request.getValuesList());
            List<Map<String, Object>> rows = null;
            if (!request.getRowsList().isEmpty()) {
                rows = request.getRowsList().stream()
                        .map(r -> {
                            Map<String, Object> map = new HashMap<>();
                            map.putAll(r.getFieldsMap());
                            return map;
                        })
                        .collect(Collectors.toList());
            }
            List<String> qiCols = request.getQiColsList().isEmpty() ? null : new ArrayList<>(request.getQiColsList());
            String ns = request.getNamespace().isEmpty() ? namespace : request.getNamespace();
            Map<String, Object> recommended = service.recommendParams(values, rows, qiCols, ns);

            String json = com.fasterxml.jackson.databind.util.StdDateFormat.class.isInstance(null) ?
                    "{}" : toJson(recommended);

            observer.onNext(RecommendResponse.newBuilder()
                    .setStatus("success")
                    .setNamespace(ns)
                    .setRecommendedParamsJson(json)
                    .build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    private String toJson(Object obj) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ─── 本地 DP / Local DP ───

    @Override
    public void perturbBinaryBatch(PerturbBinaryBatchRequest request, StreamObserver<PerturbBinaryBatchResponse> observer) {
        try {
            List<Integer> results = service.perturbBinaryBatch(new ArrayList<>(request.getValuesList()), request.getEpsilon());
            observer.onNext(PerturbBinaryBatchResponse.newBuilder().addAllResults(results).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void perturbCategoricalBatch(PerturbCategoricalBatchRequest request, StreamObserver<PerturbCategoricalBatchResponse> observer) {
        try {
            List<String> results = service.perturbCategoricalBatch(
                    new ArrayList<>(request.getValuesList()),
                    new ArrayList<>(request.getCategoriesList()),
                    request.getEpsilon());
            observer.onNext(PerturbCategoricalBatchResponse.newBuilder().addAllResults(results).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void estimateBinaryFrequency(EstimateBinaryFrequencyRequest request, StreamObserver<EstimateBinaryFrequencyResponse> observer) {
        try {
            double freq = service.estimateBinaryFrequency(new ArrayList<>(request.getReportedValuesList()), request.getEpsilon());
            observer.onNext(EstimateBinaryFrequencyResponse.newBuilder().setEstimatedFrequency(freq).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void estimateCategoricalHistogram(EstimateCategoricalHistogramRequest request, StreamObserver<EstimateCategoricalHistogramResponse> observer) {
        try {
            Map<String, Double> result = service.estimateCategoricalHistogram(
                    new ArrayList<>(request.getReportedValuesList()),
                    new ArrayList<>(request.getCategoriesList()),
                    request.getEpsilon());
            observer.onNext(EstimateCategoricalHistogramResponse.newBuilder().putAllEstimatedHistogram(result).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── 高级 DP / Advanced DP ───

    @Override
    public void dPAggregate(DPAggregateRequest request, StreamObserver<DPAggregateResponse> observer) {
        try {
            // DPAggregate 需要表格级操作，当前简化为 JSON 透传
            List<Map<String, Object>> rows = request.getRowsList().stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.putAll(r.getFieldsMap());
                        return map;
                    })
                    .collect(Collectors.toList());
            // TODO: 实现完整的 dpAggregate（需要 SDK 扩展）
            observer.onNext(DPAggregateResponse.newBuilder().setResultsJson("{}").build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPVectorSum(DPVectorSumRequest request, StreamObserver<DPVectorSumResponse> observer) {
        try {
            List<double[]> vectors = request.getVectorsList().stream()
                    .map(c -> c.getValuesList().stream().mapToDouble(Double::doubleValue).toArray())
                    .collect(Collectors.toList());
            double maxNorm = request.getMaxNorm() > 0 ? request.getMaxNorm() : 1.0;
            double epsilon = request.getEpsilon() > 0 ? request.getEpsilon() : 1.0;
            String mechanism = request.getMechanism().isEmpty() ? "gaussian" : request.getMechanism();
            double[] result = service.dpVectorSum(vectors, maxNorm, epsilon, request.getDelta(), mechanism);

            DPVectorSumResponse.Builder resp = DPVectorSumResponse.newBuilder();
            for (double v : result) resp.addNoisyVector(v);
            if (request.getReturnDetails()) {
                resp.setResultDetails(DPResultProto.newBuilder()
                        .addAllValueVector(Arrays.stream(result).boxed().collect(Collectors.toList()))
                        .setNoiseMechanism(mechanism)
                        .setEpsilonSpent(epsilon)
                        .setDeltaSpent(request.getDelta())
                        .build());
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPVectorMean(DPVectorMeanRequest request, StreamObserver<DPVectorMeanResponse> observer) {
        try {
            List<double[]> vectors = request.getVectorsList().stream()
                    .map(c -> c.getValuesList().stream().mapToDouble(Double::doubleValue).toArray())
                    .collect(Collectors.toList());
            double maxNorm = request.getMaxNorm() > 0 ? request.getMaxNorm() : 1.0;
            double epsilon = request.getEpsilon() > 0 ? request.getEpsilon() : 1.0;
            String mechanism = request.getMechanism().isEmpty() ? "gaussian" : request.getMechanism();
            double minCount = request.getMinCount() > 0 ? request.getMinCount() : 5.0;
            double[] result = service.dpVectorMean(vectors, maxNorm, epsilon, request.getDelta(), mechanism, minCount);

            DPVectorMeanResponse.Builder resp = DPVectorMeanResponse.newBuilder();
            for (double v : result) resp.addMeanVector(v);
            if (request.getReturnDetails()) {
                resp.setResultDetails(DPResultProto.newBuilder()
                        .addAllValueVector(Arrays.stream(result).boxed().collect(Collectors.toList()))
                        .setNoiseMechanism(mechanism)
                        .setEpsilonSpent(epsilon)
                        .setDeltaSpent(request.getDelta())
                        .build());
            }
            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPAdaptiveClip(DPAdaptiveClipRequest request, StreamObserver<DPAdaptiveClipResponse> observer) {
        try {
            double tq = request.getTargetQuantile() > 0 ? request.getTargetQuantile() : 0.95;
            int ni = request.getNumIterations() > 0 ? request.getNumIterations() : 15;
            double ic = request.getInitialClip() > 0 ? request.getInitialClip() : 10.0;
            double[] result = service.dpAdaptiveClip(new ArrayList<>(request.getValuesList()), request.getEpsilon(), tq, ni, ic);
            observer.onNext(DPAdaptiveClipResponse.newBuilder().setClipLower(result[0]).setClipUpper(result[1]).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    @Override
    public void dPGroupBy(DPGroupByRequest request, StreamObserver<DPGroupByResponse> observer) {
        try {
            List<Map<String, Object>> rows = request.getRowsList().stream()
                    .map(r -> {
                        Map<String, Object> map = new HashMap<>();
                        map.putAll(r.getFieldsMap());
                        return map;
                    })
                    .collect(Collectors.toList());
            Double cl = (request.getClipLower() != 0.0 || request.getClipUpper() != 0.0) ? request.getClipLower() : null;
            Double cu = (request.getClipLower() != 0.0 || request.getClipUpper() != 0.0) ? request.getClipUpper() : null;
            Map<String, Double> result = service.dpGroupBy(rows, request.getGroupCol(), request.getTargetCol(),
                    request.getAgg(), request.getEpsilon(), request.getDelta(), cl, cu, request.getMechanism());
            String json = toJson(result);
            observer.onNext(DPGroupByResponse.newBuilder().setResultJson(json).build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    // ─── 动态分类 / Dynamic Classification ───

    @Override
    public void dynClassify(DynClassificationRequest request, StreamObserver<DynClassificationResponse> observer) {
        try {
            String domain = request.getDomain().isEmpty() ? null : request.getDomain();
            String standard = request.getStandard().isEmpty() ? null : request.getStandard();
            FieldClassificationResult result = service.classifyField(
                    request.getFieldName(), request.getFieldValue(), domain, standard);

            DynClassificationResponse.Builder resp = DynClassificationResponse.newBuilder();
            String maxLevel = "";
            String engineLayer = "L1_RULE";

            if (result != null) {
                maxLevel = result.getFinalLevel() != null ? result.getFinalLevel() : "";
                engineLayer = result.getEngineLayer() != null ? result.getEngineLayer() : "L1_RULE";
                if (result.getTags() != null) {
                    for (SecurityTag tag : result.getTags()) {
                        resp.addTags(DynSecurityTagProto.newBuilder()
                                .setLevel(nullSafe(tag.getLevel()))
                                .setCategory(nullSafe(tag.getCategory()))
                                .setRuleId(nullSafe(tag.getRuleId()))
                                .setSourceEngine(nullSafe(tag.getSource()))
                                .setDomain("")
                                .setStandardId("")
                                .setIsOverride(false)
                                .setIsDowngrade(false)
                                .setMatchTarget("")
                                .build());
                    }
                }
            }
            resp.setMaxLevel(maxLevel);
            resp.setAuditTimestamp(Instant.now().toString());
            resp.setEngineLayer(engineLayer);

            observer.onNext(resp.build());
            observer.onCompleted();
        } catch (Exception e) { handleError(observer, e); }
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
