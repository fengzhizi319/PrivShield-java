"""Deployment 部署产物验证测试 / Deployment Artifact Validation Tests.

验证 Helm Chart、原生 K8s manifests、Docker Compose 编排文件以及监控配置的完整性与语法正确性：
1. Helm Chart 语法与模板渲染验证 (helm lint, helm template)
2. Kubernetes 原生 manifests 格式与结构验证
3. Docker Compose 多环境编排文件 (dev, prod, test) 语法与服务配置验证
4. Prometheus & Grafana 监控配置验证
"""

from __future__ import annotations

import json
import shutil
import subprocess
from pathlib import Path
from typing import Any

import pytest
import yaml

# 项目根目录
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
HELM_DIR = PROJECT_ROOT / "deploy" / "helm" / "PrivShield"
K8S_DIR = PROJECT_ROOT / "deploy" / "k8s"
COMPOSE_DIR = PROJECT_ROOT / "deploy" / "docker-compose"
PROMETHEUS_DIR = PROJECT_ROOT / "deploy" / "prometheus"
GRAFANA_DIR = PROJECT_ROOT / "deploy" / "grafana"

HELM = shutil.which("helm")


def _run_helm_template(
    release_name: str = "test",
    values: dict[str, Any] | None = None,
    values_files: list[Path | str] | None = None,
    extra_args: list[str] | None = None,
) -> list[dict[str, Any]]:
    """Helper to render Helm chart templates and parse output documents."""
    assert HELM is not None, "helm binary not found"
    cmd = [HELM, "template", release_name, str(HELM_DIR)]
    if values_files:
        for vf in values_files:
            cmd.extend(["-f", str(vf)])
    if extra_args:
        cmd.extend(extra_args)
    if values is not None:
        cmd.extend(["-f", "-"])
        input_data = yaml.dump(values)
    else:
        input_data = None

    result = subprocess.run(
        cmd,
        input=input_data,
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, f"helm template failed:\nSTDOUT:\n{result.stdout}\nSTDERR:\n{result.stderr}"
    return [doc for doc in yaml.safe_load_all(result.stdout) if isinstance(doc, dict)]


def _find_docs_by_kind(docs: list[dict[str, Any]], kind: str) -> list[dict[str, Any]]:
    """Filter parsed YAML documents by their Kubernetes kind."""
    return [d for d in docs if d.get("kind") == kind]


def _get_container_env_map(container: dict[str, Any]) -> dict[str, Any]:
    """Extract environment variables from a container spec into a name->value mapping."""
    env_map: dict[str, Any] = {}
    for env in container.get("env", []):
        name = env.get("name")
        if not name:
            continue
        if "value" in env:
            env_map[name] = env["value"]
        elif "valueFrom" in env:
            env_map[name] = env["valueFrom"]
    return env_map


# =============================================================================
# 1. Helm Chart 测试
# =============================================================================


@pytest.mark.skipif(HELM is None, reason="helm not found in PATH")
def test_helm_lint_default() -> None:
    """验证默认配置下 helm lint 通过。"""
    result = subprocess.run(
        [HELM, "lint", str(HELM_DIR)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


@pytest.mark.skipif(HELM is None, reason="helm not found in PATH")
def test_helm_lint_production_values() -> None:
    """验证使用 values-production.yaml 时 helm lint 通过。"""
    prod_values = HELM_DIR / "values-production.yaml"
    result = subprocess.run(
        [HELM, "lint", str(HELM_DIR), "-f", str(prod_values)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


@pytest.mark.skipif(HELM is None, reason="helm not found in PATH")
def test_helm_lint_ml_values() -> None:
    """验证使用 values-ml.yaml 时 helm lint 通过。"""
    ml_values = HELM_DIR / "values-ml.yaml"
    result = subprocess.run(
        [HELM, "lint", str(HELM_DIR), "-f", str(ml_values)],
        capture_output=True,
        text=True,
        check=False,
    )
    assert result.returncode == 0, result.stdout + result.stderr


@pytest.mark.skipif(HELM is None, reason="helm not found in PATH")
def test_helm_template_default_values() -> None:
    """验证默认 values 渲染产生包含 Java Agent 核心组件的合法 YAML。"""
    docs = _run_helm_template("test")
    assert docs, "No YAML documents returned from default helm template"

    deployments = _find_docs_by_kind(docs, "Deployment")
    assert len(deployments) == 1
    core_deploy = deployments[0]
    assert core_deploy["metadata"]["name"] == "test-privshield"
    assert core_deploy["spec"]["replicas"] == 1

    containers = core_deploy["spec"]["template"]["spec"]["containers"]
    assert len(containers) == 1
    core_container = containers[0]
    assert "privshield-java-agent" in core_container["image"]

    # 验证端口
    port_map = {p["name"]: p["containerPort"] for p in core_container.get("ports", [])}
    assert port_map.get("http") == 8079
    assert port_map.get("grpc") == 50051

    # 验证 Service
    services = _find_docs_by_kind(docs, "Service")
    assert len(services) == 1
    core_svc = services[0]
    svc_ports = {p["name"]: p["port"] for p in core_svc["spec"]["ports"]}
    assert svc_ports.get("http") == 8079
    assert svc_ports.get("grpc") == 50051


@pytest.mark.skipif(HELM is None, reason="helm not found in PATH")
def test_helm_template_production_values() -> None:
    """验证生产模式 values 渲染并包含 TLS/Auth/限速、HPA、NetworkPolicy 及 ServiceMonitor。"""
    prod_values = HELM_DIR / "values-production.yaml"
    docs = _run_helm_template(
        release_name="prod",
        values_files=[prod_values],
        extra_args=[
            "--set",
            "security.tls.existingSecret=tls-secret",
            "--set",
            "security.auth.apiKeysSecret=keys-secret",
        ],
    )

    deployments = _find_docs_by_kind(docs, "Deployment")
    assert len(deployments) == 1
    deploy = deployments[0]
    assert "replicas" not in deploy["spec"]  # 由 HPA 管理

    container = deploy["spec"]["template"]["spec"]["containers"][0]
    env_map = _get_container_env_map(container)
    assert env_map.get("PRIVACY_LOG_FORMAT") == "json"
    assert env_map.get("PRIVACY_TLS_ENABLED") == "true"
    assert env_map.get("PRIVACY_AUTH_ENABLED") == "true"
    assert env_map.get("PRIVACY_RATE_LIMIT_ENABLED") == "true"

    # 验证 HPA
    hpas = _find_docs_by_kind(docs, "HorizontalPodAutoscaler")
    assert len(hpas) == 1
    assert hpas[0]["spec"]["minReplicas"] == 2
    assert hpas[0]["spec"]["maxReplicas"] == 10

    # 验证 NetworkPolicy & ServiceMonitor
    assert len(_find_docs_by_kind(docs, "NetworkPolicy")) == 1
    assert len(_find_docs_by_kind(docs, "ServiceMonitor")) == 1


# =============================================================================
# 2. Kubernetes 原生 Manifests 测试
# =============================================================================


def test_k8s_manifests_are_valid_yaml() -> None:
    """验证 deploy/k8s/ 下所有 YAML 文件可被正确解析且包含有效文档。"""
    assert K8S_DIR.exists(), "deploy/k8s directory missing"
    yaml_files = list(K8S_DIR.glob("*.yaml"))
    assert len(yaml_files) >= 5, f"Expected at least 5 yaml files in k8s, found {len(yaml_files)}"
    for path in yaml_files:
        with path.open("r", encoding="utf-8") as f:
            docs = list(yaml.safe_load_all(f.read()))
        assert any(d is not None for d in docs), f"{path.name} contains no valid YAML documents"


def test_k8s_deployment_configuration() -> None:
    """验证 deploy/k8s/deployment.yaml 适配 Java Agent。"""
    deploy_file = K8S_DIR / "deployment.yaml"
    assert deploy_file.exists()
    with deploy_file.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    assert data["kind"] == "Deployment"
    container = data["spec"]["template"]["spec"]["containers"][0]
    assert "privshield-java-agent" in container["image"]
    ports = {p["name"]: p["containerPort"] for p in container["ports"]}
    assert ports["http"] == 8079
    assert ports["grpc"] == 50051


# =============================================================================
# 3. Docker Compose 编排测试
# =============================================================================


def test_docker_compose_files_are_valid_yaml() -> None:
    """验证 deploy/docker-compose/ 目录下所有编排文件合法。"""
    assert COMPOSE_DIR.exists()
    compose_files = [
        "docker-compose.yml",
        "docker-compose.prod.yml",
        "docker-compose.dev.yml",
        "docker-compose.test.yml",
    ]
    for filename in compose_files:
        file_path = COMPOSE_DIR / filename
        assert file_path.exists(), f"Missing compose file: {filename}"
        with file_path.open("r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
        assert isinstance(data, dict), f"{filename} not a valid YAML dict"
        assert "services" in data, f"{filename} missing 'services'"
        assert "PrivShield" in data["services"], f"{filename} missing 'PrivShield' service"


def test_docker_compose_java_agent_service() -> None:
    """验证主 docker-compose.yml 中 Java Agent 服务配置。"""
    main_compose = COMPOSE_DIR / "docker-compose.yml"
    with main_compose.open("r", encoding="utf-8") as f:
        data = yaml.safe_load(f)

    services = data["services"]
    assert "PrivShield" in services
    assert "console-backend-go" in services
    assert "console-backend-python" in services
    assert "console-web" in services

    agent_svc = services["PrivShield"]
    assert agent_svc["build"]["context"] == "../../agent"
    assert agent_svc["image"] == "privshield-java-agent:0.1.0"
    ports = agent_svc["ports"]
    assert "8079:8079" in ports
    assert "50051:50051" in ports


# =============================================================================
# 4. Prometheus & Grafana 监控配置测试
# =============================================================================


def test_prometheus_configuration_is_valid() -> None:
    """验证 deploy/prometheus/prometheus.yml 与 alerts.yml 语法。"""
    prom_file = PROMETHEUS_DIR / "prometheus.yml"
    assert prom_file.exists()
    with prom_file.open("r", encoding="utf-8") as f:
        prom_data = yaml.safe_load(f)
    assert "scrape_configs" in prom_data
    assert any(s["job_name"] == "PrivShield" for s in prom_data["scrape_configs"])

    alerts_file = PROMETHEUS_DIR / "alerts.yml"
    assert alerts_file.exists()
    with alerts_file.open("r", encoding="utf-8") as f:
        alerts_data = yaml.safe_load(f)
    assert "groups" in alerts_data
    assert len(alerts_data["groups"]) >= 3


def test_grafana_dashboard_is_valid_json() -> None:
    """验证 deploy/grafana/dashboard.json 为合法 Grafana 仪表盘 JSON。"""
    dashboard_file = GRAFANA_DIR / "dashboard.json"
    assert dashboard_file.exists()
    with dashboard_file.open("r", encoding="utf-8") as f:
        data = json.load(f)
    assert isinstance(data, dict)
    assert "panels" in data or "rows" in data or "title" in data
