# 差分隐私 — API 参考

## REST API

### POST /v1/privacy/dp/count

差分隐私计数。

**请求体:**
```json
{"values": [1,2,3,4,5], "epsilon": 1.0}
```

**响应:**
```json
{"result": 4.5}
```

### POST /v1/privacy/dp/sum

差分隐私求和。

### POST /v1/privacy/dp/mean

差分隐私均值。

## gRPC

```protobuf
rpc DPCount(DPCountRequest) returns (DPCountResponse);
rpc DPSum(DPSumRequest) returns (DPSumResponse);
rpc DPMean(DPMeanRequest) returns (DPMeanResponse);
```
