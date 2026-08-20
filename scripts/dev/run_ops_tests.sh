#!/usr/bin/env bash
# ============================================================================
# PrivShield Java — 运维与部署全套自动化测试套件
# Full Automated Operations & Deployment Test Suite for PrivShield Java
#
# 测试覆盖项：
#   1. Helm Chart 语法校验 (helm lint) 与模板渲染 (helm template)
#   2. Kubernetes 原生 Manifests (deploy/k8s/) 结构与合规性
#   3. Docker Compose 编排文件 (dev, prod, test) 语法与服务依赖
#   4. Prometheus & Grafana 监控配置与告警规则合法性
#   5. Console & Ops Shell 脚本静态语法 (bash -n) 与参数行为
#   6. Go 代理网关单元测试与健康端点契约测试
#   7. 可选端到端 (E2E) 冒烟测试
#
# 用法 / Usage:
#   ./scripts/dev/run_ops_tests.sh [--with-e2e]
# ============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

WITH_E2E=false
for arg in "$@"; do
    case "$arg" in
        --with-e2e) WITH_E2E=true ;;
    esac
done

cd "$PROJECT_ROOT"

PASSED_COUNT=0
FAILED_COUNT=0

log_section() {
    echo ""
    echo "============================================================================"
    echo "🧪 $1"
    echo "============================================================================"
}

log_pass() {
    echo "  ✅ [PASS] $1"
    PASSED_COUNT=$((PASSED_COUNT + 1))
}

log_fail() {
    echo "  ❌ [FAIL] $1"
    FAILED_COUNT=$((FAILED_COUNT + 1))
}

log_section "阶段 1：Pytest 部署与运维专项测试套件 (Helm, K8s, Compose, Scripts)"
if command -v pytest >/dev/null 2>&1; then
    if pytest tests/ -v; then
        log_pass "Pytest 部署与运维测试套件全部通过"
    else
        log_fail "Pytest 部署与运维测试套件存在失败项"
    fi
else
    echo "⚠️ 未安装 pytest，跳过 Python 自动化测试阶段"
fi

log_section "阶段 2：Go 代理后端单元与集成测试"
if command -v go >/dev/null 2>&1; then
    if (cd "$PROJECT_ROOT/console/backend-go" && go test ./...); then
        log_pass "Go 后端单元与路由测试全部通过"
    else
        log_fail "Go 后端测试存在失败项"
    fi
else
    echo "⚠️ 未安装 go，跳过 Go 测试阶段"
fi

log_section "阶段 3：Helm Lint 静态合规检测"
if command -v helm >/dev/null 2>&1; then
    if helm lint deploy/helm/PrivShield -f deploy/helm/PrivShield/values.yaml \
      && helm lint deploy/helm/PrivShield -f deploy/helm/PrivShield/values-production.yaml \
      && helm lint deploy/helm/PrivShield -f deploy/helm/PrivShield/values-ml.yaml; then
        log_pass "Helm Chart 多套 Values 静态校验全部通过"
    else
        log_fail "Helm Chart 静态校验失败"
    fi
else
    echo "⚠️ 未安装 helm，跳过 Helm Lint 阶段"
fi

if [[ "$WITH_E2E" == "true" ]]; then
    log_section "阶段 4：端到端 (E2E) 全生命周期冒烟测试"
    echo "启动临时开发服务..."
    "$PROJECT_ROOT/console/scripts/dev-start-go.sh" --force &
    DEV_PID=$!
    
    echo "等待服务就绪..."
    for i in {1..30}; do
        if curl -sf http://127.0.0.1:8081/api/health >/dev/null 2>&1; then
            echo "服务已就绪！"
            break
        fi
        sleep 1
    done

    # 验证健康端点
    if curl -sf http://127.0.0.1:8079/health | grep -q '"status":"ok"'; then
        log_pass "Java Agent /health 端点响应正常"
    else
        log_fail "Java Agent /health 端点异常"
    fi

    if curl -sf http://127.0.0.1:8081/api/health | grep -q '"backend":"ok"'; then
        log_pass "Go 后端 /api/health 端点响应正常"
    else
        log_fail "Go 后端 /api/health 端点异常"
    fi

    # 清理
    "$PROJECT_ROOT/console/scripts/dev-stop.sh" >/dev/null 2>&1 || true
fi

log_section "测试结果汇总"
echo "  通过项数: $PASSED_COUNT"
echo "  失败项数: $FAILED_COUNT"
echo "============================================================================"

if [[ "$FAILED_COUNT" -gt 0 ]]; then
    echo "❌ 运维与部署测试存在失败项，请检查上方日志！"
    exit 1
else
    echo "🎉 所有运维与部署测试均已成功通过！"
    exit 0
fi
