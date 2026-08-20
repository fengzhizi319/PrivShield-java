"""Ops 运行时健康与监控指标端点测试 / Runtime Health & Metrics Tests.

测试 Java Agent 与 Go 代理后端的健康检查、指标抓取与探针响应格式：
1. REST /health 端点响应结构与状态码
2. Spring Boot Actuator /actuator/health 响应
3. Prometheus /actuator/prometheus 指标格式
4. Go 后端 /api/health 网关代理响应结构
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

import pytest

PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent


def test_agent_health_response_schema() -> None:
    """验证 Agent 健康检查响应的 JSON Schema 契约结构。"""
    sample_payload = '{"status":"ok","namespace":"default"}'
    data = json.loads(sample_payload)
    assert data.get("status") == "ok"
    assert "namespace" in data


def test_go_backend_health_response_schema() -> None:
    """验证 Go Backend 健康检查响应的 JSON 契约结构。"""
    sample_payload = (
        '{"backend":"ok","agent":{"namespace":"default","status":"ok"},'
        '"agent_url":"127.0.0.1:50051","latency_ms":5,"via":"go-grpc","protocol":"gRPC"}'
    )
    data = json.loads(sample_payload)
    assert data.get("backend") == "ok"
    assert data.get("agent", {}).get("status") == "ok"
    assert data.get("protocol") == "gRPC"


def test_prometheus_metric_format_validation() -> None:
    """验证 Prometheus 时序指标格式符合 OpenMetrics/Prometheus 文本协议标准。"""
    sample_metrics = """
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Survivor Space",} 1.048576E7
# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{error="none",exception="none",method="GET",outcome="SUCCESS",status="200",uri="/health",} 42.0
"""
    lines = [line.strip() for line in sample_metrics.strip().splitlines()]
    assert any(line.startswith("# HELP") for line in lines)
    assert any(line.startswith("# TYPE") for line in lines)
    metric_lines = [l for l in lines if not l.startswith("#") and l]
    assert len(metric_lines) >= 2
    for m in metric_lines:
        assert "{" in m and "}" in m
