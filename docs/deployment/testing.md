# PrivShield Java — 部署与运维测试文档 (Deployment & Operations Testing)

> 本文档详细说明 **PrivShield Java** 针对容器化、Kubernetes 集群部署、Helm 发布、自动化运维脚本集及监控配置的全套自动化测试规范与执行指南。

---

## 目录

- [1. 测试架构全景](#1-测试架构全景)
- [2. 自动化测试套件分类](#2-自动化测试套件分类)
  - [2.1 Helm Chart 静态与模板渲染测试 (`tests/deployment/test_deployment.py`)](#21-helm-chart-静态与模板渲染测试)
  - [2.2 Kubernetes Manifests 结构与合规性测试](#22-kubernetes-manifests-结构与合规性测试)
  - [2.3 Docker Compose 编排文件语法与依赖测试](#23-docker-compose-编排文件语法与依赖测试)
  - [2.4 运维脚本静态检查与参数测试 (`tests/ops/test_scripts.py`)](#24-运维脚本静态检查与参数测试)
  - [2.5 Go 代理网关单元与健康探针测试 (`console/backend-go/tests/`)](#25-go-代理网关单元与健康探针测试)
- [3. 一键执行自动化测试](#3-一键执行自动化测试)
- [4. 端到端 (E2E) 冒烟与性能压测](#4-端到端-e2e-冒烟与性能压测)
- [5. 质量门禁与 CI/CD 集成](#5-质量门禁与-cicd-集成)

---

## 1. 测试架构全景

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      部署与运维自动化测试分层体系                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌─────────────────────────────────────────────────────────────────┐   │
│   │ 1. 静态合规层 (Static Linting & Syntax)                         │   │
│   │    - Shell: bash -n 语法解析、Shebang 规范、+x 权限校验         │   │
│   │    - Helm: helm lint (default / production / ml values)         │   │
│   │    - YAML/JSON: PyYAML 多文档合法性解析、Grafana JSON Schema     │   │
│   └────────────────────────────────┬────────────────────────────────┘   │
│                                    │                                    │
│   ┌────────────────────────────────▼────────────────────────────────┐   │
│   │ 2. 模板渲染与结构验证层 (Template Rendering & Manifest Assert)  │   │
│   │    - Helm Template: 校验 Deployment/Service/ConfigMap/HPA/PDB   │   │
│   │    - K8s Manifests: 校验探针路径 (/health)、端口 (8079/50051)   │   │
│   │    - Compose: 校验服务定义 (PrivShield/Go/Py/Web/vLLM) 拓扑     │   │
│   └────────────────────────────────┬────────────────────────────────┘   │
│                                    │                                    │
│   ┌────────────────────────────────▼────────────────────────────────┐   │
│   │ 3. 运行时健康契约与路由网关层 (Runtime Health & Gateway Probe) │   │
│   │    - Go 后端单元测试: HTTP Handler, LbTest, Dispatcher          │   │
│   │    - 监控指标协议: Prometheus OpenMetrics 文本格式校验          │   │
│   │    - JSON 响应契约: /health, /api/health Schema 断言            │   │
│   └────────────────────────────────┬────────────────────────────────┘   │
│                                    │                                    │
│   ┌────────────────────────────────▼────────────────────────────────┐   │
│   │ 4. 端到端集成与全生命周期冒烟层 (E2E Lifecycle & Smoke Test)    │   │
│   │    - 一键起停与 PID/端口清理 (dev-start-go.sh / dev-stop.sh)    │   │
│   │    - 链路穿透: HTTP (Web) ──▶ REST/gRPC (Go Proxy) ──▶ Agent    │   │
│   └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 自动化测试套件分类

### 2.1 Helm Chart 静态与模板渲染测试

测试文件位于 [`tests/deployment/test_deployment.py`](file:///home/charles/code/sfwork/PrivShield-java/tests/deployment/test_deployment.py)：

| 测试用例 | 验证目标 | 核心断言项 |
|---|---|---|
| `test_helm_lint_default` | 默认 Chart 语法合规 | 结构合规、无错误 |
| `test_helm_lint_production_values` | 生产 values 语法合规 | 引用一致、无语法告警 |
| `test_helm_lint_ml_values` | ML values 语法合规 | 镜像标签与资源参数正确 |
| `test_helm_template_default_values` | 默认模板渲染验证 | 包含 `privshield-java-agent`、端口 `8079` 与 `50051`、`/health` 探针 |
| `test_helm_template_production_values` | 生产模板渲染验证 | 2 副本、HPA 自动扩缩、NetworkPolicy、ServiceMonitor、JSON 结构化日志 |

### 2.2 Kubernetes Manifests 结构与合规性测试

- 验证 [`deploy/k8s/`](file:///home/charles/code/sfwork/PrivShield-java/deploy/k8s/) 下所有资源清单 (`deployment.yaml`, `service.yaml`, `configmap.yaml`, `namespace.yaml`, `kustomization.yaml`, `secret.example.yaml`) 均为有效 YAML 文档；
- 校验 Java Agent Deployment 端口映射与健康探针路径。

### 2.3 Docker Compose 编排文件语法与依赖测试

- 验证 `docker-compose.yml`、`docker-compose.dev.yml`、`docker-compose.prod.yml`、`docker-compose.test.yml` 全部为合规 YAML；
- 验证包含 `PrivShield`（基于 Java Agent 构建）、`console-backend-go`、`console-backend-python`、`console-web` 服务。

### 2.4 运维脚本静态检查与参数测试

测试文件位于 [`tests/ops/test_scripts.py`](file:///home/charles/code/sfwork/PrivShield-java/tests/ops/test_scripts.py)：

- **静态语法检查**：对 `console/scripts/`、`scripts/dev/`、`scripts/prod/` 下所有 `.sh` 脚本执行 `bash -n` 静态语法检查；
- **文件权限与 Shebang 校验**：验证脚本必须具备 `#!/usr/bin/env bash` 或 `#!/bin/bash` 且具有 `+x` 可执行权限；
- **参数行为与帮助选项**：验证 `--help`、`-h`、`--force`、`--rebuild` 参数的行为和退出码。

### 2.5 Go 代理网关单元与健康探针测试

测试文件位于 `console/backend-go/internal/...` 与 [`console/backend-go/tests/`](file:///home/charles/code/sfwork/PrivShield-java/console/backend-go/tests/)：

- 验证 `/api/health` 端点正常返回 Go 代理与 Agent 状态；
- 验证静态资源目录挂载与 SPA 回退逻辑；
- 验证 API Key 认证中间件与速率限制中间件。

---

## 3. 一键执行自动化测试

### 3.1 运行全套运维与部署测试

项目内置了一键测试脚本 [`scripts/dev/run_ops_tests.sh`](file:///home/charles/code/sfwork/PrivShield-java/scripts/dev/run_ops_tests.sh)：

```bash
# 运行静态检查、Helm 校验、K8s 验证与 Go 单元测试
./scripts/dev/run_ops_tests.sh

# 运行全套测试 + 自动拉起服务执行端到端 (E2E) 冒烟测试
./scripts/dev/run_ops_tests.sh --with-e2e
```

### 3.2 分模块独立测试命令

```bash
# 1. 运行 Pytest 部署与运维测试套件
pytest tests/ -v

# 2. 运行 Go 代理后端全部测试
cd console/backend-go && go test ./... -v

# 3. 运行 Helm Chart 静态检查
helm lint deploy/helm/PrivShield
helm lint deploy/helm/PrivShield -f deploy/helm/PrivShield/values-production.yaml

# 4. 运行前端组件与 API 测试
cd console/web && pnpm test
```

---

## 4. 端到端 (E2E) 冒烟与性能压测

### 4.1 冒烟测试流程

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "=== 执行 PrivShield Java 冒烟测试 ==="

# 1. 探测 Agent 健康状态
curl -sf http://127.0.0.1:8079/health | grep -q '"status":"ok"'
echo "✅ Agent REST 正常"

# 2. 探测 Spring Boot Actuator
curl -sf http://127.0.0.1:8079/actuator/health | grep -q '"status":"UP"'
echo "✅ Actuator 监控正常"

# 3. 探测 Go 代理后端
curl -sf http://127.0.0.1:8081/api/health | grep -q '"backend":"ok"'
echo "✅ Go 代理网关正常"

# 4. 验证核心脱敏功能
RESULT=$(curl -sf -X POST http://127.0.0.1:8079/v1/privacy/mask \
  -H "Content-Type: application/json" \
  -d '{"value": "13812345678", "field_name": "mobile"}')
echo "$RESULT" | grep -q "138\*\*\*\*5678"
echo "✅ 数据脱敏功能正常"

echo "=== 🎉 冒烟测试全部通过 ==="
```

### 4.2 高并发压力测试

```bash
# REST API 压力测试 (ApacheBench, 10,000 请求, 100 并发)
ab -n 10000 -c 100 http://127.0.0.1:8079/health

# gRPC 压力测试 (ghz)
ghz --insecure \
  --proto agent/agent-server/src/main/proto/privacy.proto \
  --call privacy.PrivacyService/Mask \
  -d '{"field_name":"mobile","value":"13812345678"}' \
  -n 10000 -c 100 127.0.0.1:50051
```

---

## 5. 质量门禁与 CI/CD 集成

在持续集成 (CI) 流水线中，建议配置如下执行门禁阶段：

```yaml
# CI 门禁任务示例 (.github/workflows/ops-test.yml)
jobs:
  ops-validation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Set up Go
        uses: actions/setup-go@v5
        with:
          go-version: '1.23'
      - name: Set up Helm
        uses: azure/setup-helm@v4
      - name: Set up Python
        uses: actions/setup-python@v5
        with:
          python-version: '3.11'
      - name: Install Python Test Dependencies
        run: pip install pytest pyyaml
      - name: Run Full Ops & Deployment Tests
        run: ./scripts/dev/run_ops_tests.sh
```

